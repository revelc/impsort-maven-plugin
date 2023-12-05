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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import com.github.javaparser.ParserConfiguration.LanguageLevel;

public class AbstractImpSortMojoTest {

  @Test
  public void testLanguageLevel() {
    assertSame(LanguageLevel.POPULAR, getLanguageLevel(null, false));
    assertSame(LanguageLevel.POPULAR, getLanguageLevel("", false));

    assertSame(LanguageLevel.POPULAR, getLanguageLevel(null, true));
    assertSame(LanguageLevel.POPULAR, getLanguageLevel("", true));
    assertSame(LanguageLevel.POPULAR, getLanguageLevel("JAVA_11", true));

    var prefix = "JAVA_";
    var previewSuffix = "_PREVIEW";
    // only these can be represented using single digit or 1.<digit>, as in Java 5 or Java 1.5
    var expectedOneDotOrSingle = new TreeSet<>(Set.of("5", "6", "7", "8", "9"));
    Arrays.stream(LanguageLevel.values()).forEach(level -> {
      assertTrue(level.name().startsWith(prefix));
      String version = level.name().substring(prefix.length());
      if (!version.endsWith(previewSuffix)) {
        version = version.replace('_', '.');
      }
      if (version.length() == 1) {
        // check that the versions that can be represented by either X or 1.X are the same
        assertTrue(expectedOneDotOrSingle.remove(version), "Unexpectedly saw " + version);
        assertSame(level, getLanguageLevel("1." + version, false));
      } else if (!version.contains(".") && !version.contains(previewSuffix)) {
        // make sure versions above Java 9 can't be represented as 1.X, as in 1.10
        final var v = version;
        assertThrows(IllegalArgumentException.class, () -> getLanguageLevel("1." + v, false));
      }
      // basic check to make sure our getter method returns the correct level
      assertSame(level, getLanguageLevel(version, false));
    });
    assertTrue(expectedOneDotOrSingle.isEmpty(), "Did not encounter " + expectedOneDotOrSingle);
  }

}
