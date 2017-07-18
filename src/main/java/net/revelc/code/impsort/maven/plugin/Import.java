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

import static java.util.Objects.requireNonNull;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Import {

  private static final Pattern PATTERN = Pattern.compile(
      "^(?<prefix>.*?)\\s*(?<type>import(?:\\s+static)?)\\s+(?<item>\\w+(?:\\s*[.]\\s*\\w+)*(?:\\s*[.]\\s*[*])?)\\s*;(?<suffix>.*)$", Pattern.MULTILINE);

  private final boolean isStatic;
  private final String imp;
  private final String prefix;
  private final String suffix;

  private Import(String type, String imp, String prefix, String suffix) {
    this.isStatic = requireNonNull(type).endsWith("static");
    this.imp = requireNonNull(imp);
    this.prefix = prefix == null ? "" : prefix;
    this.suffix = suffix == null ? "" : suffix;
  }

  boolean isStatic() {
    return isStatic;
  }

  String getImport() {
    return imp;
  }

  String getPrefix() {
    return prefix;
  }

  String getSuffix() {
    return suffix;
  }

  @Override
  public String toString() {
    return "import" + (isStatic() ? " static" : "") + " " + getImport() + ";" + suffix;
  }

  static Optional<Import> parse(String line) {
    Matcher m = Import.PATTERN.matcher(line);
    if (m.find()) {
      String type = m.group("type").replaceAll("\\s+", " ");
      String item = m.group("item").replaceAll("\\s+", "");
      String prefix = m.group("prefix").replaceAll("^\\s+", "");
      String suffix = m.group("suffix").replaceAll("\\s+$", "");
      return Optional.of(new Import(type, item, prefix, suffix));
    }
    return Optional.empty();
  }

  @Override
  public int hashCode() {
    return (getImport().hashCode() * 2) + (isStatic ? 1 : 0);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (obj instanceof Import) {
      Import o2 = (Import) obj;
      return isStatic == o2.isStatic && imp.equals(o2.imp) && prefix.equals(o2.prefix) && suffix.equals(o2.suffix);
    }
    return false;
  }

}
