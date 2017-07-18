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

import java.util.TreeSet;

class ImportOrganizer {

  private final TreeSet<Import> imports;

  ImportOrganizer(String groups, String staticGroups, boolean staticAfter, boolean joinStaticWithNonStatic) {
    imports = new TreeSet<>(new ImportComparator(groups, staticGroups, staticAfter));
  }

  void add(Import imp) {
    imports.add(imp);
  }

  boolean isEmpty() {
    return imports.isEmpty();
  }

}
