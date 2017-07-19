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

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import org.junit.Test;

public class ImpSortTest {

  @Test
  public void testSort() throws IOException {
    Path p = Paths.get(System.getProperty("user.dir"), "src", "it", "plugin-test", "src", "test", "java", "net", "revelc", "code", "imp", "PluginIT.java");
    new ImpSort("java.,javax.,org.,com.,*", "*", false, false).parseFile(p);
  }

  @Test
  public void parseGroups() {
    assertEquals(Arrays.asList(new Group("*", 0)), Group.parse("*"));
    assertEquals(Arrays.asList(new Group("*", 0)), Group.parse(""));
    assertEquals(Arrays.asList(new Group("a", 0), new Group("*", 1)), Group.parse("a"));
    assertEquals(Arrays.asList(new Group("com.", 3), new Group("java", 4), new Group("ab", 2), new Group("b", 0), new Group("*", 1), new Group("a", 5)),
        Group.parse(" b , * , ab ,com., java , a"));
  }

}
