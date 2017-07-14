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

package net.revelc.code.impsort.maven.plugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

@Mojo(name = "sort", defaultPhase = LifecyclePhase.PROCESS_TEST_SOURCES, threadSafe = true, requiresDependencyResolution = ResolutionScope.NONE)
public class SortMojo extends AbstractImpSortMojo {
  private Pattern pattern = Pattern.compile("^\\s*(?<type>import(?:\\s+static)?)\\s+(?<item>\\w+(?:\\s*[.]\\s*\\w+)*(?:\\s*[.]\\s*[*])?)\\s*;(?<suffix>.*)$");

  @Override
  public void processFile(File f) throws MojoFailureException {

    ArrayList<String> before = new ArrayList<>();
    TreeSet<String> imports = new TreeSet<>();
    TreeSet<String> staticImports = new TreeSet<>();
    ArrayList<String> after = new ArrayList<>();

    Path path = f.toPath();
    getLog().warn("Reading file " + path);
    try (Stream<String> lines = Files.lines(path)) {
      lines.forEachOrdered(line -> {
        Matcher m = pattern.matcher(line);
        if (m.find()) {
          String type = m.group("type").replaceAll("\\s+", " ");
          String item = m.group("item").replaceAll("\\s+", "");
          String suffix = m.group("suffix").replaceAll("\\s+$", "");
          String imp = type + " " + item + ";" + suffix;

          if (type.endsWith("static")) {
            staticImports.add(imp);
          } else {
            imports.add(imp);
          }
          getLog().warn("Found import: " + imp); // should be debug, but leave warn for testing
        } else if (imports.isEmpty() && staticImports.isEmpty()) {
          before.add(line);
        } else {
          after.add(line);
        }
      });
    } catch (IOException e) {
      fail("Error reading file " + f, e);
    }
  }

}
