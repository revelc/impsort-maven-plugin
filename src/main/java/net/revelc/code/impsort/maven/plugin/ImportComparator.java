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

import static java.util.Objects.requireNonNull;

import java.util.Comparator;

public class ImportComparator implements Comparator<Import> {

  private final String groups;
  private final String staticGroups;
  private final boolean staticAfter;

  public ImportComparator(String groups, String staticGroups, boolean staticAfter) {
    this.groups = requireNonNull(groups);
    this.staticGroups = requireNonNull(staticGroups);
    this.staticAfter = staticAfter;
  }

  @Override
  public int compare(Import o1, Import o2) {
    requireNonNull(o1);
    requireNonNull(o2);

    if (o1 == o2) {
      return 0;
    }

    if (o1.isStatic() != o2.isStatic()) {
      return staticAfter == o1.isStatic() ? 1 : -1;
    }

    int compareImps = groupCompare(o1.isStatic() ? staticGroups : groups, o1.getImport(), o2.getImport());
    if (compareImps != 0) {
      return compareImps;
    }

    int comparePrefix = o1.getPrefix().compareTo(o2.getPrefix());
    if (comparePrefix != 0) {
      return comparePrefix;
    }

    int compareSuffix = o1.getSuffix().compareTo(o2.getSuffix());
    if (compareSuffix != 0) {
      return compareSuffix;
    }

    return 0;
  }

  private int groupCompare(String groups, String import1, String import2) {
    if ("*".equals(groups)) {
      return import1.compareTo(import2);
    }
    throw new UnsupportedOperationException("Groups are not yet supported");
  }

}
