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
import static org.junit.Assert.assertThrows;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Test class for special file cases.
 */
public class LineEndingEdgeCasesTest {

  private static Grouper eclipseDefaults =
      new Grouper("java.,javax.,org.,com.", "", false, false, true);

  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  /**
   * Test successfully parsing empty file (zero bytes).
   */
  @Test
  public void testEmptyFile() throws IOException {
    Path p = folder.newFile("EmptyFile.java").toPath();
    Files.write(p, new byte[0]);
    EmptyFileException e = assertThrows(EmptyFileException.class,
        () -> new ImpSort(StandardCharsets.UTF_8, eclipseDefaults, true, true, LineEnding.AUTO)
            .parseFile(p));
    assertEquals("Empty file " + p, e.getMessage());
  }

  /**
   * Test successfully parsing file without any line ending.
   */
  @Test
  public void testFileWithoutLineEnding() throws IOException {
    String s =
        "import java.lang.System;public class FileWithoutNewline{public static void main(String[] args){System.out.println(\"Hello, world!\");}}";
    Path p = folder.newFile("FileWithoutLineEnding.java").toPath();
    Files.write(p, s.getBytes(StandardCharsets.UTF_8));
    UnknownLineEndingException e = assertThrows(UnknownLineEndingException.class,
        () -> new ImpSort(StandardCharsets.UTF_8, eclipseDefaults, true, true, LineEnding.AUTO)
            .parseFile(p));
    assertEquals("Unknown line ending for file " + p, e.getMessage());
  }

  /**
   * Test successfully parsing file that can't be parsed.
   */
  @Test
  public void testInvalidFile() throws IOException {
    String s = "public class InvalidFile {\n" + "    public static void main(String[] args) {\n"
        + "        System.out.println(\"Hello, world!\")\n" + "    }\n" + "}\n";
    Path p = folder.newFile("InvalidFile.java").toPath();
    Files.write(p, s.getBytes(StandardCharsets.UTF_8));
    IOException e = assertThrows(IOException.class,
        () -> new ImpSort(StandardCharsets.UTF_8, eclipseDefaults, true, true, LineEnding.AUTO)
            .parseFile(p));
    assertEquals("Unable to parse " + p, e.getMessage());
  }

}
