/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.revelc.code.impsort;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.javaparser.JavaParser;
import com.github.javaparser.JavaToken;
import com.github.javaparser.Position;
import com.github.javaparser.TokenRange;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.modules.ModuleDeclaration;

public class ImpSort {

  private static final Comparator<Node> BY_POSITION = (a, b) -> a.getBegin().get().compareTo(b.getBegin().get());

  private final Grouper grouper;
  private final boolean removeUnused;

  public ImpSort(final Grouper grouper, final boolean removeUnused) {
    this.grouper = grouper;
    this.removeUnused = removeUnused;
  }

  public Result parseFile(final Path path) throws IOException {
    List<String> fileLines = Files.readAllLines(path);
    CompilationUnit unit = JavaParser.parse(String.join("\n", fileLines));

    Position packagePosition = unit.getPackageDeclaration().map(p -> p.getEnd().get()).orElse(unit.getBegin().get());
    NodeList<ImportDeclaration> importDeclarations = unit.getImports();
    if (importDeclarations.isEmpty()) {
      return new Result(path, fileLines, 0, fileLines.size(), "", "", Collections.emptyList());
    }

    // find orphaned comments before between package and last import
    Position lastImportPosition = importDeclarations.stream().max(BY_POSITION).get().getBegin().get();
    Stream<Comment> orphanedComments = unit.getOrphanComments().parallelStream().filter(c -> {
      Position p = c.getBegin().get();
      return p.isAfter(packagePosition) && p.isBefore(lastImportPosition);
    });

    // create entire import section (with interspersed comments)
    List<Node> importSectionNodes = Stream.concat(orphanedComments, importDeclarations.stream()).collect(Collectors.toList());
    importSectionNodes.sort(BY_POSITION);
    // position line numbers start at 1, not 0
    int start = importSectionNodes.get(0).getBegin().get().line - 1;
    int stop = importSectionNodes.get(importSectionNodes.size() - 1).getEnd().get().line;
    // get the original import section lines from the file
    // include surrounding whitespace
    while (start > 0 && fileLines.get(start - 1).trim().isEmpty()) {
      --start;
    }
    while (stop < fileLines.size() && fileLines.get(stop).trim().isEmpty()) {
      ++stop;
    }
    String originalSection = String.join("\n", fileLines.subList(start, stop)) + "\n";

    Set<Import> allImports = convertImportSection(importSectionNodes);

    if (removeUnused) {
      removeUnusedImports(allImports, extractTokens(unit));
    }

    String newSection = grouper.groupedImports(allImports);
    if (start > 0) {
      // add newline before imports, as long as imports not at start of file
      newSection = "\n" + newSection;
    }
    if (stop < fileLines.size()) {
      // add newline after imports, as long as there's more in file
      newSection += "\n";
    }

    return new Result(path, fileLines, start, stop, originalSection, newSection, allImports);
  }

  // return imports, with associated comments, in order found in the file
  private static Set<Import> convertImportSection(List<Node> importSectionNodes) {
    List<Comment> recentComments = new ArrayList<>();
    LinkedHashSet<Import> allImports = new LinkedHashSet<>(importSectionNodes.size() / 2);
    for (Node node : importSectionNodes) {
      if (node instanceof Comment) {
        recentComments.add((Comment) node);
      } else if (node instanceof ImportDeclaration) {
        List<Node> thisImport = new ArrayList<>(2);
        ImportDeclaration impDecl = (ImportDeclaration) node;
        thisImport.addAll(recentComments);

        Optional<Comment> impComment = impDecl.getComment();
        if (impComment.isPresent()) {
          Comment c = impComment.get();
          if (c.getBegin().get().isBefore(impDecl.getBegin().get())) {
            thisImport.add(c);
            thisImport.add(impDecl);
          } else {
            thisImport.add(impDecl);
            thisImport.add(c);
          }
        } else {
          thisImport.add(impDecl);
        }

        recentComments.clear();
        convertAndAddImport(allImports, thisImport);
      } else {
        throw new IllegalStateException("Unknown node: " + node);
      }
    }
    if (!recentComments.isEmpty()) {
      throw new IllegalStateException("Unexpectedly found more orphaned comments: " + recentComments);
    }
    return allImports;
  }

  private static void convertAndAddImport(LinkedHashSet<Import> allImports, List<Node> thisImport) {
    boolean isStatic = false;
    String importItem = null;
    String prefix = "";
    String suffix = "";
    for (Node n : thisImport) {
      if (n instanceof Comment) {
        if (importItem == null) {
          prefix += n.toString();
        } else {
          suffix += n.toString();
        }
      }
      if (n instanceof ImportDeclaration) {
        ImportDeclaration i = (ImportDeclaration) n;
        isStatic = i.isStatic();
        importItem = i.getName().asString() + (i.isAsterisk() ? ".*" : "");
      }
    }
    suffix = suffix.trim();
    if (!suffix.isEmpty()) {
      suffix = " " + suffix;
    }
    Import imp = new Import(isStatic, importItem, prefix.trim(), suffix);
    Iterator<Import> iter = allImports.iterator();
    // this de-duplication can probably be made more efficient by doing it all at the end
    while (iter.hasNext()) {
      Import candidate = iter.next(); // potential duplicate
      if (candidate.isDuplicatedBy(imp)) {
        iter.remove();
        imp = candidate.combineWith(imp);
      }
    }
    allImports.add(imp);
  }

  /*
   * Extract all of the tokens from the main body of the file.
   *
   * This set of tokens represents all of the file's dependencies, and is used
   * to figure out whether or not an import is unused.
   */
  private static Set<String> extractTokens(CompilationUnit unit) {
    Set<String> tokens = new HashSet<String>();
    NodeList<TypeDeclaration<?>> types = unit.getTypes();
    types.forEach(node -> {
      Optional<TokenRange> tokenRange = node.getTokenRange();
      if (tokenRange.isPresent() && tokenRange.get() != TokenRange.INVALID) {
        Iterator<JavaToken> iterator = tokenRange.get().iterator();
        while (iterator.hasNext()) {
          String token = iterator.next().asString();
          if (!token.isEmpty() && Character.isJavaIdentifierStart(token.charAt(0))) {
            tokens.add(token);
          }
        }
      }
    });

    return tokens;
  }

  /*
   * Remove unused imports.
   *
   * This algorithm only looks at the file itself, and evaluates whether or not
   * a given import is unused, by checking if the last segment of the import path
   * (typically a class name or a static function name) appears in the file.
   *
   * This means that it is not possible to remove import statements with wildcards.
   */
  private static void removeUnusedImports(Set<Import> imports, Set<String> tokens) {
    imports.removeIf(i -> {
      String[] segments = i.getImport().split("[.]");
      if (segments.length == 0) {
        throw new AssertionError("Parse tree includes invalid import statements");
      }

      String lastSegment = segments[segments.length - 1];
      if (lastSegment.equals("*")) return false;

      return !tokens.contains(lastSegment);
    });
  }
}
