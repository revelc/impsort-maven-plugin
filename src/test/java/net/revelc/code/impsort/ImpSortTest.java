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
import java.util.Comparator;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;

import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.expr.Name;
import com.google.common.primitives.Bytes;

public class ImpSortTest {

  private static Grouper eclipseDefaults =
      new Grouper("java.,javax.,org.,com.", "", false, false, true);

  private TreeSet<Import> addTestImportsForSort(Comparator<Import> comparator) {
    TreeSet<Import> set = new TreeSet<>(comparator);
    set.add(new Import(true, "p.MyClass.A", "", ""));
    set.add(new Import(true, "p.MyClass.B.A", "", ""));
    set.add(new Import(true, "p.MyClass.B.B", "", ""));
    set.add(new Import(true, "p.MyClass.C.A.A", "", ""));
    set.add(new Import(true, "p.MyClass.C.A.B", "", ""));
    set.add(new Import(true, "p.MyClass.C.B", "", ""));
    set.add(new Import(true, "p.MyClass.D", "", ""));
    return set;
  }

  @Test
  public void testDepthFirstComparator() {
    TreeSet<Import> set = addTestImportsForSort(Grouper.depthFirstComparator);
    assertArrayEquals(
        new String[] {"p.MyClass.A", "p.MyClass.B.A", "p.MyClass.B.B", "p.MyClass.C.A.A",
            "p.MyClass.C.A.B", "p.MyClass.C.B", "p.MyClass.D"},
        set.stream().sequential().map(imp -> imp.getImport()).toArray());
  }

  @Test
  public void testBreadthFirstComparator() {
    TreeSet<Import> set = addTestImportsForSort(Grouper.breadthFirstComparator);
    assertArrayEquals(
        new String[] {"p.MyClass.A", "p.MyClass.D", "p.MyClass.B.A", "p.MyClass.B.B",
            "p.MyClass.C.B", "p.MyClass.C.A.A", "p.MyClass.C.A.B"},
        set.stream().sequential().map(imp -> imp.getImport()).toArray());
  }

  @Test
  public void testSort() throws IOException {
    Path p = Paths.get(System.getProperty("user.dir"), "src", "test", "resources",
        "BasicPluginTests.java");
    new ImpSort(StandardCharsets.UTF_8, eclipseDefaults, false, true).parseFile(p);
  }

  @Test
  public void testUnused() throws IOException {
    Path p =
        Paths.get(System.getProperty("user.dir"), "src", "test", "resources", "UnusedImports.java");
    Result result = new ImpSort(StandardCharsets.UTF_8, eclipseDefaults, true, true).parseFile(p);
    Set<String> imports =
        result.getImports().stream().map(Import::getImport).collect(Collectors.toSet());
    assertEquals(20, imports.size());
    assertEquals(20, result.getImports().size());

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
    assertTrue(imports.contains("com.foo.Type11"));
    assertTrue(imports.contains("com.foo.internal.Type12"));
    assertTrue(imports.contains("com.foo.params.Type13"));

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
  public void testEmptyJavadoc() throws IOException {
    Path p =
        Paths.get(System.getProperty("user.dir"), "src", "test", "resources", "EmptyJavadoc.java");
    Result result = new ImpSort(StandardCharsets.UTF_8, eclipseDefaults, true, true).parseFile(p);
    Set<String> imports = new HashSet<>();
    for (Import i : result.getImports()) {
      imports.add(i.getImport());
    }

    assertTrue(imports.contains("java.util.List"));
    assertTrue(imports.contains("java.util.ArrayList"));
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
    Result result =
        new ImpSort(StandardCharsets.ISO_8859_1, eclipseDefaults, true, true).parseFile(p);
    assertTrue(result.getImports().isEmpty());
    Path output = File.createTempFile("impSort", null).toPath();
    result.saveSorted(output);
    byte[] testData = Files.readAllBytes(p);
    // ensure expected ISO_8859_1 byte is present in test data, this defends against file being
    // wrongly encoded if edited
    assertTrue(Bytes.contains(testData, (byte) 0xe9));
    assertArrayEquals(testData, Files.readAllBytes(output));
  }

  @Test
  public void testRemoveSamePackageImports() {
    Set<Import> imports = Stream
        .of(new Import(false, "abc.Blah", "", ""), new Import(false, "abcd.ef.Blah.Blah", "", ""),
            new Import(false, "abcd.ef.Blah2", "", ""), new Import(false, "abcd.efg.Blah2", "", ""))
        .collect(Collectors.toSet());
    assertEquals(4, imports.size());
    assertTrue(imports.stream().anyMatch(imp -> "abc.Blah".equals(imp.getImport())));
    assertTrue(imports.stream().anyMatch(imp -> "abcd.ef.Blah2".equals(imp.getImport())));
    assertTrue(imports.stream().anyMatch(imp -> "abcd.ef.Blah.Blah".equals(imp.getImport())));
    assertTrue(imports.stream().anyMatch(imp -> "abcd.efg.Blah2".equals(imp.getImport())));
    ImpSort.removeSamePackageImports(imports, Optional.empty());
    assertEquals(4, imports.size());
    assertTrue(imports.stream().anyMatch(imp -> "abc.Blah".equals(imp.getImport())));
    assertTrue(imports.stream().anyMatch(imp -> "abcd.ef.Blah2".equals(imp.getImport())));
    assertTrue(imports.stream().anyMatch(imp -> "abcd.ef.Blah.Blah".equals(imp.getImport())));
    assertTrue(imports.stream().anyMatch(imp -> "abcd.efg.Blah2".equals(imp.getImport())));
    ImpSort.removeSamePackageImports(imports,
        Optional.of(new PackageDeclaration(new Name("abcd.ef"))));
    assertEquals(3, imports.size());
    assertTrue(imports.stream().anyMatch(imp -> "abc.Blah".equals(imp.getImport())));
    assertFalse(imports.stream().anyMatch(imp -> "abcd.ef.Blah2".equals(imp.getImport())));
    assertTrue(imports.stream().anyMatch(imp -> "abcd.ef.Blah.Blah".equals(imp.getImport())));
    assertTrue(imports.stream().anyMatch(imp -> "abcd.efg.Blah2".equals(imp.getImport())));
    ImpSort.removeSamePackageImports(imports, Optional.of(new PackageDeclaration(new Name("abc"))));
    assertEquals(2, imports.size());
    assertFalse(imports.stream().anyMatch(imp -> "abc.Blah".equals(imp.getImport())));
    assertFalse(imports.stream().anyMatch(imp -> "abcd.ef.Blah2".equals(imp.getImport())));
    assertTrue(imports.stream().anyMatch(imp -> "abcd.ef.Blah.Blah".equals(imp.getImport())));
    assertTrue(imports.stream().anyMatch(imp -> "abcd.efg.Blah2".equals(imp.getImport())));
  }
}
