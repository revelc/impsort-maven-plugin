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

import static net.revelc.code.impsort.maven.plugin.AbstractImpSortMojo.getLanguageLevel;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

import com.github.javaparser.ParserConfiguration.LanguageLevel;

public class AbstractImpSortMojoTest {

  @Test
  public void testLanguageLevel() {
    assertSame(LanguageLevel.POPULAR, getLanguageLevel(null));
    assertSame(LanguageLevel.POPULAR, getLanguageLevel(""));
    assertSame(LanguageLevel.JAVA_1_0, getLanguageLevel("1.0"));
    assertSame(LanguageLevel.JAVA_1_1, getLanguageLevel("1.1"));
    assertSame(LanguageLevel.JAVA_1_2, getLanguageLevel("1.2"));
    assertSame(LanguageLevel.JAVA_1_3, getLanguageLevel("1.3"));
    assertSame(LanguageLevel.JAVA_1_4, getLanguageLevel("1.4"));
    assertSame(LanguageLevel.JAVA_5, getLanguageLevel("1.5"));
    assertSame(LanguageLevel.JAVA_5, getLanguageLevel("5"));
    assertSame(LanguageLevel.JAVA_6, getLanguageLevel("1.6"));
    assertSame(LanguageLevel.JAVA_6, getLanguageLevel("6"));
    assertSame(LanguageLevel.JAVA_7, getLanguageLevel("1.7"));
    assertSame(LanguageLevel.JAVA_7, getLanguageLevel("7"));
    assertSame(LanguageLevel.JAVA_8, getLanguageLevel("1.8"));
    assertSame(LanguageLevel.JAVA_8, getLanguageLevel("8"));
    assertSame(LanguageLevel.JAVA_9, getLanguageLevel("1.9"));
    assertSame(LanguageLevel.JAVA_9, getLanguageLevel("9"));
    assertSame(LanguageLevel.JAVA_10, getLanguageLevel("10"));
    assertSame(LanguageLevel.JAVA_11, getLanguageLevel("11"));
    assertSame(LanguageLevel.JAVA_12, getLanguageLevel("12"));
    assertSame(LanguageLevel.JAVA_13, getLanguageLevel("13"));
    assertSame(LanguageLevel.JAVA_14, getLanguageLevel("14"));
    assertThrows(IllegalArgumentException.class, () -> getLanguageLevel("1.10"));
    assertThrows(IllegalArgumentException.class, () -> getLanguageLevel("1.11"));
    assertThrows(IllegalArgumentException.class, () -> getLanguageLevel("1.12"));
    assertThrows(IllegalArgumentException.class, () -> getLanguageLevel("1.13"));
    assertThrows(IllegalArgumentException.class, () -> getLanguageLevel("1.14"));
  }

}
