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
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

@Mojo(name = "sort", defaultPhase = LifecyclePhase.PROCESS_TEST_SOURCES, threadSafe = true, requiresDependencyResolution = ResolutionScope.NONE)
public class SortMojo extends AbstractImpSortMojo {

  @Override
  public void processFile(File f) throws MojoFailureException {

    ArrayList<String> before = new ArrayList<>();
    ImportOrganizer imports = new ImportOrganizer(groups, staticGroups, staticAfter, joinStaticWithNonStatic);
    ArrayList<String> after = new ArrayList<>();

    Path path = f.toPath();
    getLog().debug("Reading file " + path);
    try (Stream<String> lines = Files.lines(path)) {
      lines.forEachOrdered(line -> {
        Optional<Import> imp = Import.parse(line);
        if (imp.isPresent()) {
          Import i = imp.get();
          imports.add(i);
          getLog().debug("Found import: " + i);
        } else if (imports.isEmpty()) {
          before.add(line);
        } else {
          after.add(line);
        }
      });
    } catch (IOException e) {
      fail("Error reading file " + f, e);
    }

    sort(path, before, imports, after);
  }

  private void sort(Path path, ArrayList<String> before, ImportOrganizer imports, ArrayList<String> after) {

    // remove trailing blank lines before imports
    while (!before.isEmpty() && before.get(before.size() - 1).trim().isEmpty()) {
      before.remove(before.size() - 1);
    }

    // remove leading blank lines after imports.
    while (!after.isEmpty() && after.get(0).trim().isEmpty()) {
      after.remove(0);
    }

  }

}
