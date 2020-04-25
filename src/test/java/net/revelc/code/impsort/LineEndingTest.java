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
import org.junit.Test;

/**
 * Test class for {@link LineEnding}.
 *
 * This file is taken from <a href="https://github.com/revelc/formatter-maven-plugin/blob/1ff06fc6ba4062cb080915a9422fc0fb4ef1699a/src/test/java/net/revelc/code/formatter/LineEndingTest.java">formatter-maven-plugin</a>.
 */
public class LineEndingTest {

  /**
   * Test successfully determining CRLF line ending.
   */
  @Test
  public void test_success_read_line_endings_crlf() throws Exception {
    String fileData = "Test\r\nTest\r\nTest\r\n";
    LineEnding lineEnd = LineEnding.determineLineEnding(fileData);
    assertEquals(LineEnding.CRLF, lineEnd);
  }

  /**
   * Test successfully determining LF line ending.
   */
  @Test
  public void test_success_read_line_endings_lf() throws Exception {
    String fileData = "Test\nTest\nTest\n";
    LineEnding lineEnd = LineEnding.determineLineEnding(fileData);
    assertEquals(LineEnding.LF, lineEnd);
  }

  /**
   * Test successfully determining CR line ending.
   */
  @Test
  public void test_success_read_line_endings_cr() throws Exception {
    String fileData = "Test\rTest\rTest\r";
    LineEnding lineEnd = LineEnding.determineLineEnding(fileData);
    assertEquals(LineEnding.CR, lineEnd);
  }

  /**
   * Test successfully determining LF line ending with mixed endings.
   */
  @Test
  public void test_success_read_line_endings_mixed_lf() throws Exception {
    String fileData = "Test\r\nTest\rTest\nTest\nTest\r\nTest\n";
    LineEnding lineEnd = LineEnding.determineLineEnding(fileData);
    assertEquals(LineEnding.LF, lineEnd);
  }

  /**
   * Test successfully determining AUTO line ending with mixed endings and no clear majority.
   */
  @Test
  public void test_success_read_line_endings_mixed_auto() throws Exception {
    String fileData = "Test\r\nTest\r\nTest\nTest\nTest\r\nTest\nTest\r";
    LineEnding lineEnd = LineEnding.determineLineEnding(fileData);
    assertEquals(LineEnding.UNKNOWN, lineEnd);
  }

  /**
   * Test successfully determining AUTO line ending with no endings.
   */
  @Test
  public void test_success_read_line_endings_none_auto() throws Exception {
    String fileData = "TestTestTestTest";
    LineEnding lineEnd = LineEnding.determineLineEnding(fileData);
    assertEquals(LineEnding.UNKNOWN, lineEnd);
  }

}
