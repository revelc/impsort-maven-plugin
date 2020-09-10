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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import net.revelc.code.impsort.ex.ImpSortException;
import net.revelc.code.impsort.ex.ImpSortException.Reason;

/**
 * Test class for special file cases.
 */
public class LineEndingEdgeCasesTest {

  private static Grouper eclipseDefaults =
      new Grouper("java.,javax.,org.,com.", "", false, false, true);

  @Rule
  public TemporaryFolder folder =
      new TemporaryFolder(new File(System.getProperty("user.dir"), "target"));

  /**
   * Test successfully parsing empty file (zero bytes).
   */
  @Test
  public void testEmptyFile() throws IOException {
    Path p = folder.newFile("EmptyFile.java").toPath();
    Files.write(p, new byte[0]);
    Result actual = new ImpSort(UTF_8, eclipseDefaults, true, true, LineEnding.AUTO).parseFile(p);
    assertEquals(Result.EMPTY_FILE, actual);
    assertTrue(actual.getImports().isEmpty());
    assertTrue(actual.isSorted());
  }

  /**
   * Test successfully parsing file without any line ending, but line ending config set to KEEP.
   */
  @Test
  public void testFileKeepWithoutLineEnding() throws IOException {
    String s =
        "import java.lang.System;public class FileWithoutNewline{public static void main(String[] args){System.out.println(\"Hello, world!\");}}";
    Path p = folder.newFile("FileWithoutLineEnding.java").toPath();
    Files.write(p, s.getBytes(UTF_8));
    ImpSortException e = assertThrows(ImpSortException.class,
        () -> new ImpSort(UTF_8, eclipseDefaults, true, true, LineEnding.KEEP).parseFile(p));
    assertTrue(e.getReason() == Reason.UNKNOWN_LINE_ENDING);
    assertEquals("file: " + p + "; reason: unknown line ending", e.getMessage());
  }

  /**
   * Test successfully parsing file without any line ending, and line ending config set to AUTO.
   */
  @Test
  public void testFileAutoWithoutLineEnding() throws IOException {
    String s =
        "import java.lang.System;public class FileWithoutNewline{public static void main(String[] args){System.out.println(\"Hello, world!\");}}";
    Path p = folder.newFile("FileWithoutLineEnding.java").toPath();
    Files.write(p, s.getBytes(UTF_8));
    Result result = new ImpSort(UTF_8, eclipseDefaults, true, true, LineEnding.AUTO).parseFile(p);
    assertEquals(1, result.getImports().size());
    assertEquals("java.lang.System", result.getImports().iterator().next().getImport());
    assertTrue(result.getImports().iterator().next().getPrefix().isEmpty());
    assertTrue(result.getImports().iterator().next().getSuffix().isEmpty());
    assertFalse(result.getImports().iterator().next().isStatic());
    assertFalse(result.isSorted());
  }

  /**
   * Test when the parser partially parses the file and creates a partial result.
   */
  @Test
  public void testPartiallyParsedFile() throws IOException {
    String s = "public class InvalidFile {\n" + "    public static void main(String[] args) {\n"
        + "        System.out.println(\"Hello, world!\")\n" + "    }\n" + "}\n";
    Path p = folder.newFile("InvalidFile.java").toPath();
    Files.write(p, s.getBytes(UTF_8));
    ImpSortException e = assertThrows(ImpSortException.class,
        () -> new ImpSort(UTF_8, eclipseDefaults, true, true, LineEnding.AUTO).parseFile(p));
    assertTrue(e.getReason() == Reason.PARTIAL_PARSE);
    assertEquals("file: " + p + "; reason: the Java file contained parse errors", e.getMessage());
  }

  /**
   * Test when the parser can't parse the file at all.
   */
  @Test
  public void testInvalidFile() throws IOException {
    String s = "\0\n\n";
    Path p = folder.newFile("NoResult.java").toPath();
    Files.write(p, s.getBytes(UTF_8));
    ImpSortException e = assertThrows(ImpSortException.class,
        () -> new ImpSort(UTF_8, eclipseDefaults, true, true, LineEnding.AUTO).parseFile(p));
    assertTrue(e.getReason() == Reason.UNABLE_TO_PARSE);
    assertEquals("file: " + p + "; reason: unable to successfully parse the Java file",
        e.getMessage());
  }

  /**
   * Test when the file doesn't exist.
   */
  @Test
  public void testMissingFile() throws IOException {
    Path p = new File(folder.getRoot().getAbsolutePath(), "MissingFile.java").toPath();
    NoSuchFileException e = assertThrows(NoSuchFileException.class,
        () -> new ImpSort(UTF_8, eclipseDefaults, true, true, LineEnding.AUTO).parseFile(p));
    assertEquals(p.toString(), e.getMessage());
  }
}
