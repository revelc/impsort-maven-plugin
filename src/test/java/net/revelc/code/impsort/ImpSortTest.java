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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import com.google.common.primitives.Bytes;

public class ImpSortTest {

  private static Grouper eclipseDefaults = new Grouper("java.,javax.,org.,com.", "", false, false);

  @Test
  public void testSort() throws IOException {
    Path p = Paths.get(System.getProperty("user.dir"), "src", "it", "plugin-test", "src", "test",
        "java", "net", "revelc", "code", "imp", "PluginIT.java");
    new ImpSort(StandardCharsets.UTF_8, eclipseDefaults, false).parseFile(p);
  }

  @Test
  public void testUnused() throws IOException {
    Path p =
        Paths.get(System.getProperty("user.dir"), "src", "test", "resources", "UnusedImports.java");
    Result result = new ImpSort(StandardCharsets.UTF_8, eclipseDefaults, true).parseFile(p);
    Set<String> imports = new HashSet<>();
    for (Import i : result.getImports()) {
      imports.add(i.getImport());
    }

    assertTrue(imports.contains("com.foo.Type1"));
    assertTrue(imports.contains("com.foo.Type2"));
    assertTrue(imports.contains("com.foo.Type3"));
    assertTrue(imports.contains("com.foo.Type4"));
    assertTrue(imports.contains("com.foo.Type5"));
    assertTrue(imports.contains("com.foo.Type6"));
    assertTrue(imports.contains("com.foo.Type7"));
    assertTrue(imports.contains("com.foo.Type8"));
    assertTrue(imports.contains("com.foo.Type9"));
    assertTrue(imports.contains("com.foo.Type10"));

    assertFalse(imports.contains("com.google.common.base.Predicates"));
    assertTrue(imports.contains("com.google.common.collect.ImmutableMap"));
    assertFalse(imports.contains("io.swagger.annotations.ApiOperation"));
    assertFalse(imports.contains("java.util.ArrayList"));
    assertTrue(imports.contains("java.util.HashMap"));
    assertTrue(imports.contains("java.util.List"));
    assertTrue(imports.contains("java.util.Map"));
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
    assertEquals(
        Arrays.asList(new Group("com.", 3), new Group("java", 4), new Group("ab", 2),
            new Group("b", 0), new Group("*", 1), new Group("a", 5)),
        Grouper.parse(" b , * , ab ,com., java , a"));
  }

  @Test
  public void testIso8859ForIssue3() throws IOException {
    Path p =
        Paths.get(System.getProperty("user.dir"), "src", "test", "resources", "Iso8859File.java");
    Result result = new ImpSort(StandardCharsets.ISO_8859_1, eclipseDefaults, true).parseFile(p);
    assertTrue(result.getImports().isEmpty());
    Path output = File.createTempFile("impSort", null).toPath();
    result.saveSorted(output);
    byte[] testData = Files.readAllBytes(p);
    // ensure expected ISO_8859_1 byte is present in test data, this defends against file being
    // wrongly encoded if edited
    assertTrue(Bytes.contains(testData, (byte) 0xe9));
    assertArrayEquals(testData, Files.readAllBytes(output));
  }
}
