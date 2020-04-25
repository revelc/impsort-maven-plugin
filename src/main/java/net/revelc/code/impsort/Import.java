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

import java.util.Objects;

public class Import {

  private final boolean isStatic;
  private final String imp;
  private final String prefix;
  private final String suffix;
  private final String eol;

  Import(final boolean isStatic, final String imp, final String prefix, final String suffix,
      final String eol) {
    this.isStatic = isStatic;
    this.imp = Objects.requireNonNull(imp);
    this.prefix = Objects.requireNonNull(prefix);
    this.suffix = Objects.requireNonNull(suffix);
    this.eol = eol;
  }

  public boolean isStatic() {
    return isStatic;
  }

  public String getImport() {
    return imp;
  }

  public String getPrefix() {
    return prefix;
  }

  public String getSuffix() {
    return suffix;
  }

  @Override
  public String toString() {
    return prefix + (prefix.isEmpty() ? "" : eol) + "import" + (isStatic() ? " static" : "") + " "
        + getImport() + ";" + suffix;
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
      return isStatic == o2.isStatic && imp.contentEquals(o2.imp) && prefix.contentEquals(o2.prefix)
          && suffix.contentEquals(o2.suffix);
    }
    return false;
  }

  public boolean isDuplicatedBy(Import other) {
    return isStatic() == other.isStatic() && getImport().contentEquals(other.getImport());
  }

  public Import combineWith(Import duplicate) {
    String newPrefix;
    String newSuffix;
    if (getPrefix().isEmpty()) {
      newPrefix = duplicate.getPrefix();
    } else if (duplicate.getPrefix().isEmpty()) {
      newPrefix = getPrefix();
    } else {
      newPrefix = getPrefix() + eol + duplicate.getPrefix();
    }
    if (getSuffix().isEmpty()) {
      newSuffix = duplicate.getSuffix();
    } else if (duplicate.getSuffix().isEmpty()) {
      newSuffix = getSuffix();
    } else {
      newSuffix = getSuffix() + duplicate.getSuffix();
    }
    return new Import(isStatic(), getImport(), newPrefix, newSuffix, eol);
  }

}
