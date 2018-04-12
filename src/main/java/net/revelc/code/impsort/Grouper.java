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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class Grouper {

  private final List<Group> groups;
  private final List<Group> staticGroups;
  private final boolean staticAfter;
  private final boolean joinStaticWithNonStatic;

  public Grouper(String groups, String staticGroups, boolean staticAfter,
      boolean joinStaticWithNonStatic) {
    this.groups = Collections.unmodifiableList(parse(groups));
    this.staticGroups = parse(staticGroups);
    this.staticAfter = staticAfter;
    this.joinStaticWithNonStatic = joinStaticWithNonStatic;
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

  public Map<Integer, ArrayList<Import>> groupNonStatic(Collection<Import> allImports) {
    return group(allImports, groups, i -> !i.isStatic());
  }

  public Map<Integer, ArrayList<Import>> groupStatic(Collection<Import> allImports) {
    return group(allImports, staticGroups, i -> i.isStatic());
  }

  private static Map<Integer, ArrayList<Import>> group(Collection<Import> allImports,
      List<Group> groups, Predicate<Import> filter) {
    Map<Integer, ArrayList<Import>> map = new TreeMap<>();
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

  public boolean getStaticAfter() {
    return staticAfter;
  }

  public boolean getJoinStaticWithNonStatic() {
    return joinStaticWithNonStatic;
  }

  public String groupedImports(Collection<Import> allImports) {
    StringBuilder sb = new StringBuilder();
    Map<Integer, ArrayList<Import>> staticImports = groupStatic(allImports);
    Map<Integer, ArrayList<Import>> nonStaticImports = groupNonStatic(allImports);

    Map<Integer, ArrayList<Import>> first = getStaticAfter() ? nonStaticImports : staticImports;
    Map<Integer, ArrayList<Import>> second = getStaticAfter() ? staticImports : nonStaticImports;

    AtomicBoolean firstGroup = new AtomicBoolean(true);
    Consumer<ArrayList<Import>> consumer = grouping -> {
      if (!firstGroup.getAndSet(false)) {
        sb.append("\n");
      }
      grouping.forEach(imp -> sb.append(imp).append("\n"));
    };
    first.values().forEach(consumer);
    if (!getJoinStaticWithNonStatic() && !first.isEmpty() && !second.isEmpty()) {
      sb.append("\n");
    }
    firstGroup.set(true);
    second.values().forEach(consumer);

    // allImports.forEach(x -> System.out.print("-----\n" + x + "\n-----"));
    return sb.toString();
  }

}
