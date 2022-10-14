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

import static com.github.javaparser.javadoc.JavadocBlockTag.Type.EXCEPTION;
import static com.github.javaparser.javadoc.JavadocBlockTag.Type.THROWS;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.logging.Log;

import com.github.javaparser.JavaParser;
import com.github.javaparser.JavaToken;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ParserConfiguration.LanguageLevel;
import com.github.javaparser.Position;
import com.github.javaparser.TokenRange;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.javadoc.Javadoc;
import com.github.javaparser.javadoc.JavadocBlockTag;
import com.github.javaparser.javadoc.description.JavadocDescription;
import com.github.javaparser.javadoc.description.JavadocInlineTag;
import com.github.javaparser.javadoc.description.JavadocSnippet;

import net.revelc.code.impsort.ex.ImpSortException;
import net.revelc.code.impsort.ex.ImpSortException.Reason;

public class ImpSort {

  private static final Comparator<Node> BY_POSITION = Comparator.comparing(a -> a.getBegin().get());

  private final Charset sourceEncoding;
  private final Grouper grouper;
  private final boolean removeUnused;
  private final boolean treatSamePackageAsUnused;
  private final LineEnding lineEnding;
  private final LanguageLevel languageLevel;
  private final Log logger;

  public ImpSort(final Charset sourceEncoding, final Grouper grouper, final boolean removeUnused,
      final boolean treatSamePackageAsUnused, final LineEnding lineEnding, final Log logger) {
    this(sourceEncoding, grouper, removeUnused, treatSamePackageAsUnused, lineEnding,
        LanguageLevel.POPULAR, logger);
  }

  public ImpSort(final Charset sourceEncoding, final Grouper grouper, final boolean removeUnused,
      final boolean treatSamePackageAsUnused, final LineEnding lineEnding,
      final LanguageLevel languageLevel, final Log logger) {
    this.sourceEncoding = sourceEncoding;
    this.grouper = grouper;
    this.removeUnused = removeUnused;
    this.treatSamePackageAsUnused = treatSamePackageAsUnused;
    this.lineEnding = lineEnding;
    this.languageLevel = languageLevel;
    this.logger = logger;
  }

  private static List<String> readAllLines(String str) {
    List<String> result = new ArrayList<>();
    try (Scanner s = new Scanner(str)) {
      while (s.hasNextLine()) {
        result.add(s.nextLine());
      }
    }
    return result;
  }

  /**
   * Parses the file denoted by this path and returns the result.
   *
   * @param path the path
   * @return the result
   * @throws IOException if the Java file denoted by this path can't be parsed
   */
  public Result parseFile(final Path path) throws IOException {
    byte[] buf = Files.readAllBytes(path);
    return parseFile(path, buf);
  }

  public Result parseFile(final Path path, final byte[] buf) throws IOException {
    if (buf == null || buf.length == 0) {
      return Result.EMPTY_FILE;
    }
    String file = new String(buf, sourceEncoding);
    LineEnding fileLineEnding = LineEnding.determineLineEnding(file);
    LineEnding impLineEnding;
    if (lineEnding == LineEnding.KEEP) {
      if (fileLineEnding == LineEnding.UNKNOWN) {
        throw new ImpSortException(path, Reason.UNKNOWN_LINE_ENDING);
      }
      impLineEnding = fileLineEnding;
    } else {
      impLineEnding = lineEnding;
    }
    List<String> fileLines = readAllLines(file);
    ParseResult<CompilationUnit> parseResult =
        new JavaParser(new ParserConfiguration().setLanguageLevel(languageLevel)).parse(file);
    CompilationUnit unit = parseResult.getResult().orElseThrow(() -> {
      parseResult.getProblems().forEach(problem -> logger.error(problem.toString()));
      return new ImpSortException(path, Reason.UNABLE_TO_PARSE,
              StringUtils.join(parseResult.getProblems(), System.lineSeparator()));
    });
    if (!parseResult.isSuccessful()) {
      parseResult.getProblems().forEach(problem -> logger.error(problem.toString()));
      throw new ImpSortException(path, Reason.PARTIAL_PARSE,
              StringUtils.join(parseResult.getProblems(), System.lineSeparator()));
    }
    Position packagePosition =
        unit.getPackageDeclaration().map(p -> p.getEnd().get()).orElse(unit.getBegin().get());
    NodeList<ImportDeclaration> importDeclarations = unit.getImports();
    if (importDeclarations.isEmpty()) {
      return new Result(path, sourceEncoding, fileLines, 0, fileLines.size(), "", "",
          Collections.emptyList(), impLineEnding);
    }

    // find orphaned comments before between package and last import
    Position lastImportPosition =
        importDeclarations.stream().max(BY_POSITION).get().getBegin().get();
    Stream<Comment> orphanedComments = unit.getOrphanComments().parallelStream().filter(c -> {
      Position p = c.getBegin().get();
      return p.isAfter(packagePosition) && p.isBefore(lastImportPosition);
    });

    // create entire import section (with interspersed comments)
    List<Node> importSectionNodes =
        Stream.concat(orphanedComments, importDeclarations.stream()).collect(Collectors.toList());
    importSectionNodes.sort(BY_POSITION);
    // position line numbers start at 1, not 0
    Node firstImport = importSectionNodes.get(0);
    int start = firstImport.getComment().map(c -> c.getBegin().get())
        .orElse(firstImport.getBegin().get()).line - 1;
    int stop = importSectionNodes.get(importSectionNodes.size() - 1).getEnd().get().line;
    // get the original import section lines from the file
    // include surrounding whitespace
    while (start > 0 && fileLines.get(start - 1).trim().isEmpty()) {
      --start;
    }
    while (stop < fileLines.size() && fileLines.get(stop).trim().isEmpty()) {
      ++stop;
    }
    String originalSection = String.join(impLineEnding.getChars(), fileLines.subList(start, stop))
        + impLineEnding.getChars();

    Set<Import> allImports = convertImportSection(importSectionNodes, impLineEnding.getChars());

    if (removeUnused) {
      removeUnusedImports(allImports, tokensInUse(unit));
      if (treatSamePackageAsUnused) {
        removeSamePackageImports(allImports, unit.getPackageDeclaration());
      }
    }

    String newSection = grouper.groupedImports(allImports, impLineEnding.getChars());
    if (start > 0) {
      // add newline before imports, as long as imports not at start of file
      newSection = impLineEnding.getChars() + newSection;
    }
    if (stop < fileLines.size()) {
      // add newline after imports, as long as there's more in file
      newSection += impLineEnding.getChars();
    }

    return new Result(path, sourceEncoding, fileLines, start, stop, originalSection, newSection,
        allImports, impLineEnding);
  }

  // return imports, with associated comments, in order found in the file
  private static Set<Import> convertImportSection(List<Node> importSectionNodes, String eol) {
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
        convertAndAddImport(allImports, thisImport, eol);
      } else {
        throw new IllegalStateException("Unknown node: " + node);
      }
    }
    if (!recentComments.isEmpty()) {
      throw new IllegalStateException(
          "Unexpectedly found more orphaned comments: " + recentComments);
    }
    return allImports;
  }

  private static void convertAndAddImport(LinkedHashSet<Import> allImports, List<Node> thisImport,
      String eol) {
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
    Import imp = new Import(isStatic, importItem, prefix.trim(), suffix, eol);
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
   * This set of tokens represents all of the file's dependencies, and is used to figure out whether
   * or not an import is unused.
   */
  private static Set<String> tokensInUse(CompilationUnit unit) {

    // Extract tokens from the java code:
    Stream<Node> packageDecl =
        unit.getPackageDeclaration().isPresent()
            ? Stream.of(unit.getPackageDeclaration().get()).map(PackageDeclaration::getAnnotations)
                .flatMap(NodeList::stream)
            : Stream.empty();
    Stream<String> typesInCode = Stream.concat(packageDecl, unit.getTypes().stream())
        .map(Node::getTokenRange).filter(Optional::isPresent).map(Optional::get)
        .filter(r -> r != TokenRange.INVALID).flatMap(r -> {
          // get all JavaTokens as strings from each range
          return StreamSupport.stream(r.spliterator(), false);
        }).map(JavaToken::asString);

    // Extract referenced class names from parsed javadoc comments:
    Stream<String> typesInJavadocs = unit.getAllComments().stream()
        .filter(c -> c instanceof JavadocComment).map(JavadocComment.class::cast)
        .map(JavadocComment::parse).flatMap(ImpSort::parseJavadoc);

    return Stream.concat(typesInCode, typesInJavadocs)
        .filter(t -> t != null && !t.isEmpty() && Character.isJavaIdentifierStart(t.charAt(0)))
        .collect(Collectors.toSet());
  }

  // parse both main doc description and any block tags
  private static Stream<String> parseJavadoc(Javadoc javadoc) {
    // parse main doc description
    Stream<String> stringsFromJavadocDescription =
        Stream.of(javadoc.getDescription()).flatMap(ImpSort::parseJavadocDescription);
    // grab tag names and parsed descriptions for block tags
    Stream<String> stringsFromBlockTags = javadoc.getBlockTags().stream().flatMap(tag -> {
      // only @throws and @exception have names who are importable; @param and others don't
      EnumSet<JavadocBlockTag.Type> blockTagTypesWithImportableNames =
          EnumSet.of(THROWS, EXCEPTION);
      Stream<String> importableTagNames = blockTagTypesWithImportableNames.contains(tag.getType())
          ? Stream.of(tag.getName()).filter(Optional::isPresent).map(Optional::get)
          : Stream.empty();
      Stream<String> tagDescriptions =
          Stream.of(tag.getContent()).flatMap(ImpSort::parseJavadocDescription);
      return Stream.concat(importableTagNames, tagDescriptions);
    });
    return Stream.concat(stringsFromJavadocDescription, stringsFromBlockTags);
  }

  private static Stream<String> parseJavadocDescription(JavadocDescription description) {
    return description.getElements().stream().map(element -> {
      if (element instanceof JavadocInlineTag) {
        // inline tags like {@link Foo}
        return ((JavadocInlineTag) element).getContent();
      } else if (element instanceof JavadocSnippet) {
        // snippets like @see Foo
        return element.toText();
      } else {
        // try to handle unknown elements as best we can
        return element.toText();
      }
    }).flatMap(s -> {
      // split text descriptions into word tokens
      return Stream.of(s.split("\\W+"));
    });
  }

  /*
   * Remove unused imports.
   *
   * This algorithm only looks at the file itself, and evaluates whether or not a given import is
   * unused, by checking if the last segment of the import path (typically a class name or a static
   * function name) appears in the file.
   *
   * This means that it is not possible to remove import statements with wildcards.
   */
  private static void removeUnusedImports(Set<Import> imports, Set<String> tokensInUse) {
    imports.removeIf(i -> {
      String[] segments = i.getImport().split("[.]");
      if (segments.length == 0) {
        throw new AssertionError("Parse tree includes invalid import statements");
      }

      String lastSegment = segments[segments.length - 1];
      if (lastSegment.equals("*")) {
        return false;
      }

      return !tokensInUse.contains(lastSegment);
    });
  }

  static void removeSamePackageImports(Set<Import> imports,
      Optional<PackageDeclaration> packageDeclaration) {
    String packageName = packageDeclaration.map(p -> p.getName().toString()).orElse("");
    imports.removeIf(i -> {
      String imp = i.getImport();
      if (packageName.isEmpty()) {
        return !imp.contains(".");
      }
      return imp.startsWith(packageName) && imp.lastIndexOf(".") <= packageName.length();
    });

  }

}
