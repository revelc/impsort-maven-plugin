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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

public class ParserTest {

  private static Parser parser = new Parser();
  private static Grouper eclipseDefaults = new Grouper("java.,javax.,org.,com.", "", false, false);

  @Test
  public void testUnused() throws IOException {
    Path p = Paths.get(System.getProperty("user.dir"), "src", "test", "resources", "UnusedImports.java");
    Parser.Result result = parser.parseFile(p);
    result.removeUnusedImports();
    Set<String> imports = new HashSet<>();
    for (Import i : result.getImports()) {
      imports.add(i.getImport());
    }
    assertFalse(imports.contains("com.google.common.base.Predicates"));
    assertTrue(imports.contains("com.google.common.collect.ImmutableMap"));
    assertFalse(imports.contains("io.swagger.annotations.ApiOperation"));
    assertFalse(imports.contains("java.util.ArrayList"));
    assertTrue(imports.contains("java.util.List"));
    assertFalse(imports.contains("org.springframework.beans.factory.annotation.Autowired"));
    assertTrue(imports.contains("org.springframework.stereotype.Component"));
    assertFalse(imports.contains("org.junit.Assert.assertEquals"));
    assertTrue(imports.contains("org.junit.Assert.assertFalse"));
    assertTrue(imports.contains("org.junit.Assert.*"));
  }

  @Test
  public void parseGroups() {
    assertEquals(Arrays.asList(new Group("*", 0)), Grouper.parse("*"));
    assertEquals(Arrays.asList(new Group("*", 0)), Grouper.parse(""));
    assertEquals(Arrays.asList(new Group("a", 0), new Group("*", 1)), Grouper.parse("a"));
    assertEquals(Arrays.asList(new Group("com.", 3), new Group("java", 4), new Group("ab", 2), new Group("b", 0), new Group("*", 1), new Group("a", 5)),
        Grouper.parse(" b , * , ab ,com., java , a"));
  }

}
