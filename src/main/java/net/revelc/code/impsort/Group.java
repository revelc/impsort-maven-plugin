/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.revelc.code.impsort;

import java.util.Objects;

final class Group {

  private final String groupPrefix;
  private final int order;

  Group(final String groupPrefix, final int encounterOrder) {
    this.groupPrefix = Objects.requireNonNull(groupPrefix);
    this.order = encounterOrder;
    if (this.order < 0) {
      throw new IllegalArgumentException("Encounter order cannot be negative");
    }
  }

  public String getPrefix() {
    return groupPrefix;
  }

  public int getOrder() {
    return order;
  }

  public boolean matches(final String importClass) {
    return "*".contentEquals(getPrefix()) || importClass.startsWith(getPrefix());
  }

  @Override
  public int hashCode() {
    return getPrefix().hashCode() + getOrder();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }

    if (obj instanceof Group) {
      Group g = (Group) obj;
      return getPrefix().contentEquals(g.getPrefix()) && getOrder() == g.getOrder();
    }

    return false;
  }

}
