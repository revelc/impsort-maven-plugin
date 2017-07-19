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
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ImpSort {

  private static final Pattern PATTERN = Pattern.compile(
      "^(?<prefix>.*?)\\s*\\b(?<type>import(?:\\s+static)?)\\s+(?<item>\\w+(?:\\s*[.]\\s*\\w+)*(?:\\s*[.]\\s*[*])?)\\s*;(?<suffix>[^\\n]*)",
      Pattern.MULTILINE | Pattern.DOTALL);
  private static final Pattern SPACES = Pattern.compile("\\s+");
  private static final Pattern TRAILING_SPACES = Pattern.compile("\\s+$");
  private static final Pattern LEADING_SPACES = Pattern.compile("^\\s+");
  private static final Pattern BLANK_LINES = Pattern.compile("\\n\\s*\\n");
  private static final Pattern UP_TO_LAST_BLANK_LINE = Pattern.compile(".*(?:\\n|^)\\s*(?:\\n|$)", Pattern.DOTALL);
  private static final Pattern UP_TO_PACKAGE = Pattern.compile(".*(?:\\n|^)\\s*package\\s+.*?;.*?(?:\\n|$)", Pattern.DOTALL);
  private static final Pattern AFTER_CLASS_START = Pattern.compile("\\b(?:class|interface|enum)\\b");

  final ArrayList<Group> groups;
  final ArrayList<Group> staticGroups;
  final boolean staticAfter;
  final boolean joinStaticWithNonStatic;

  public ImpSort(final String groups, final String staticGroups, final boolean staticAfter, final boolean joinStaticWithNonStatic) {
    this.groups = Group.parse(groups);
    this.staticGroups = Group.parse(staticGroups);
    this.staticAfter = staticAfter;
    this.joinStaticWithNonStatic = joinStaticWithNonStatic;
  }

  public Result parseFile(final Path path) throws IOException {
    final String fileContents = String.join("\n", Files.readAllLines(path));
    Set<Import> allImports = new LinkedHashSet<>();
    Matcher m = PATTERN.matcher(fileContents);
    int firstMatch = -1;
    int lastPosition = 0;
    while (m.find()) {
      String prefix = LEADING_SPACES.matcher(m.group("prefix")).replaceAll(""); // remove beginning whitespace from prefix

      // deal with the first import specially
      if (allImports.isEmpty()) {
        firstMatch = m.start();
        String prefixOriginal = prefix;
        // cut off anything prior to the last blank line
        prefix = UP_TO_LAST_BLANK_LINE.matcher(prefix).replaceAll("");
        // remove everything up to and including package statement if it exists
        prefix = UP_TO_PACKAGE.matcher(prefix).replaceAll("");
        prefix = prefix.trim();
        firstMatch += prefixOriginal.length() - prefix.length();
      } else {
        // remove blank lines in prefix
        prefix = BLANK_LINES.matcher(prefix).replaceAll("\n").trim();
      }
      if (AFTER_CLASS_START.matcher(prefix).find()) {
        // matches after class start are false-positives
        break;
      }

      boolean isStatic = m.group("type").contains("static");
      String imp = SPACES.matcher(m.group("item")).replaceAll(""); // remove spaces from class
      String suffix = TRAILING_SPACES.matcher(m.group("suffix")).replaceAll(""); // remove trailing whitespace from suffix

      allImports.add(new Import(isStatic, imp, prefix, suffix));
      lastPosition = m.end();
    }

    // DEBUG printing
    // allImports.forEach(i -> System.out.print("---\n" + i + "\n---"));
    // Stream.of(allImports.iterator().next()).forEach(i -> System.out.print("---\n" + i + "\n---"));
    return new Result(this, fileContents, firstMatch, lastPosition, allImports);
  }

}
