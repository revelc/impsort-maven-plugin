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

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Objects;
import java.util.regex.Pattern;

class Group {

  private final String groupPrefix;
  private final int order;

  Group(final String groupPrefix, final int encounterOrder) {
    this.groupPrefix = Objects.requireNonNull(groupPrefix);
    this.order = encounterOrder;
    if (this.order < 0) {
      throw new IllegalArgumentException("Encounter order cannot be negative");
    }
  }

  public String getPrefix() {
    return groupPrefix;
  }

  public int getOrder() {
    return order;
  }

  public boolean matches(final String importClass) {
    return "*".contentEquals(getPrefix()) || importClass.startsWith(getPrefix());
  }

  @Override
  public int hashCode() {
    return getPrefix().hashCode() + getOrder();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }

    if (obj instanceof Group) {
      Group g = (Group) obj;
      return getPrefix().contentEquals(g.getPrefix()) && getOrder() == g.getOrder();
    }

    return false;
  }

  static ArrayList<Group> parse(String groups) {
    ArrayList<Group> parsedGroups = new ArrayList<>();
    String[] array = requireNonNull(groups).replaceAll("\\s+", "").split(",");
    Pattern validGroup = Pattern.compile("^(?:\\w+(?:[.]\\w+)*[.]?|[*])$");
    // skip special case where the first element from split is empty and is the only element
    if (array.length != 1 || !array[0].isEmpty()) {
      for (String g : array) {
        if (!validGroup.matcher(g).matches()) {
          throw new IllegalArgumentException("Invalid group (" + g + ") in (" + groups + ")");
        }
        if (parsedGroups.stream().anyMatch(o -> g.contentEquals(o.getPrefix()))) {
          throw new IllegalArgumentException("Duplicate group (" + g + ") in (" + groups + ")");
        }

        int encounterOrder = parsedGroups.size();
        parsedGroups.add(new Group(g, encounterOrder));
      }
    }
    // include the default group if not already included
    if (parsedGroups.stream().noneMatch(o -> "*".contentEquals(o.getPrefix()))) {
      parsedGroups.add(new Group("*", parsedGroups.size()));
    }
    parsedGroups.sort((a, b) -> {
      // sort in reverse prefix length order first, then encounter order
      int comp = Integer.compare(b.getPrefix().length(), a.getPrefix().length());
      return comp != 0 ? comp : Integer.compare(a.getOrder(), a.getOrder());
    });
    return parsedGroups;
  }

}
