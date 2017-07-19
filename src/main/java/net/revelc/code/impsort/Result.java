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
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class Result {

  private final String originalFileContents;
  private final String newFileContents;
  private final Iterable<Import> allImports;

  Result(final ImpSort impSort, final String originalFileContents, final int firstMatch, final int lastPosition, final Set<Import> allImports) {
    this.originalFileContents = originalFileContents;
    this.allImports = Collections.unmodifiableSet(allImports);
    if (allImports.isEmpty()) {
      this.newFileContents = originalFileContents;
    } else {
      // append file header
      StringBuilder sb = new StringBuilder(originalFileContents.substring(0, firstMatch).replaceAll("(?:\\n\\s*)+$", ""));
      sb.append("\n\n");
      buildImportSection(impSort, sb, allImports);
      sb.append("\n");
      // append rest of file
      sb.append(originalFileContents.substring(lastPosition, originalFileContents.length()).replaceAll("^(?:\\n\\s*)+", ""));
      this.newFileContents = sb.toString();
    }
  }

  private static void buildImportSection(ImpSort impSort, StringBuilder sb, Set<Import> allImports) {
    Map<Integer,ArrayList<Import>> staticImports = buildImportSection(allImports, impSort.staticGroups, i -> i.isStatic());
    Map<Integer,ArrayList<Import>> nonStaticImports = buildImportSection(allImports, impSort.groups, i -> !i.isStatic());

    boolean staticAfter = impSort.staticAfter;
    Map<Integer,ArrayList<Import>> first = staticAfter ? nonStaticImports : staticImports;
    Map<Integer,ArrayList<Import>> second = staticAfter ? staticImports : nonStaticImports;

    AtomicBoolean firstGroup = new AtomicBoolean(true);
    Consumer<ArrayList<Import>> consumer = grouping -> {
      if (!firstGroup.getAndSet(false)) {
        sb.append("\n");
      }
      grouping.forEach(imp -> sb.append(imp).append("\n"));
    };
    first.values().forEach(consumer);
    boolean joinStaticWithNonStatic = impSort.joinStaticWithNonStatic;
    if (!joinStaticWithNonStatic && !first.isEmpty() && !second.isEmpty()) {
      sb.append("\n");
    }
    firstGroup.set(true);
    second.values().forEach(consumer);

    // allImports.forEach(x -> System.out.print("-----\n" + x + "\n-----"));
  }

  private static Map<Integer,ArrayList<Import>> buildImportSection(Set<Import> allImports, ArrayList<Group> groups, Predicate<Import> filter) {
    Map<Integer,ArrayList<Import>> map = new TreeMap<>();
    allImports.stream().filter(filter).forEach(imp -> {
      for (Group group : groups) {
        if (group.matches(imp.getImport())) {
          map.computeIfAbsent(group.getOrder(), x -> new ArrayList<>()).add(imp);
          break;
        }
      }
    });
    for (ArrayList<Import> list : map.values()) {
      list.sort((a, b) -> a.getImport().compareTo(b.getImport()));
    }
    return map;
  }

  public boolean isSorted() {
    return originalFileContents.contentEquals(newFileContents);
  }

  public Iterable<Import> getImports() {
    return allImports;
  }

  public void saveBackup(Path destination) throws IOException {
    writeFile(originalFileContents, destination);
  }

  public void saveSorted(Path destination) throws IOException {
    writeFile(newFileContents, destination);
  }

  private void writeFile(String contents, Path destination) throws IOException {
    Files.write(destination, Arrays.asList(contents.split("\\n")));
  }

}
