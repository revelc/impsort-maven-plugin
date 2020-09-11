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

import static java.nio.file.Files.newOutputStream;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class Result {

  private Boolean isSorted;

  private final Path path;
  private final Charset sourceEncoding;
  private final String originalSection;
  private final String newSection;
  private final Collection<Import> allImports;
  private final List<String> fileLines;
  private final int start;
  private final int stop;
  private final LineEnding lineEnding;

  public static final Result EMPTY_FILE =
      new Result(null, null, null, 0, 0, "", "", Collections.emptyList(), null);

  Result(Path path, Charset sourceEncoding, List<String> fileLines, int start, int stop,
      String originalSection, String newSection, Collection<Import> allImports,
      LineEnding lineEnding) {
    this.path = path;
    this.sourceEncoding = sourceEncoding;
    this.originalSection = originalSection;
    this.newSection = newSection;
    this.allImports = allImports;
    this.fileLines = fileLines;
    this.start = start;
    this.stop = stop;
    this.lineEnding = lineEnding;
  }

  public boolean isSorted() {
    if (isSorted == null) {
      isSorted = originalSection.contentEquals(newSection);
    }
    return isSorted;
  }

  public Collection<Import> getImports() {
    return allImports;
  }

  public void saveBackup(Path destination) throws IOException {
    Files.copy(path, destination, StandardCopyOption.REPLACE_EXISTING);
  }

  public void saveSorted(Path destination) throws IOException {
    if (isSorted()) {
      if (!Files.isSameFile(path, destination)) {
        saveBackup(destination);
      }
      return;
    }
    List<String> beforeImports = fileLines.subList(0, start);
    List<String> importLines = Arrays.asList(newSection.split(lineEnding.getChars()));
    List<String> afterImports = fileLines.subList(stop, fileLines.size());
    List<String> allLines =
        new ArrayList<>(beforeImports.size() + importLines.size() + afterImports.size() + 1);
    allLines.addAll(beforeImports);
    allLines.addAll(importLines);
    if (afterImports.size() > 0) {
      allLines.add(""); // restore blank line lost by split
    }
    allLines.addAll(afterImports);
    writeLines(destination, allLines, sourceEncoding);
  }

  private Path writeLines(Path destination, List<String> lines, Charset sourceEncoding)
      throws IOException {
    try (OutputStream out = newOutputStream(destination);
        BufferedWriter writer =
            new BufferedWriter(new OutputStreamWriter(out, sourceEncoding.newEncoder()))) {
      for (String line : lines) {
        writer.write(line);
        writer.write(lineEnding.getChars());
      }
    }
    return destination;
  }

}
