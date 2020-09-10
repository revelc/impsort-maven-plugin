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

package net.revelc.code.impsort.ex;

import java.io.IOException;
import java.nio.file.Path;

public class ImpSortException extends IOException {

  public enum Reason {
    // @formatter:off
    UNKNOWN_LINE_ENDING("unknown line ending"),
    UNABLE_TO_PARSE("unable to successfully parse the Java file"),
    PARTIAL_PARSE("the Java file contained parse errors")
    // @formatter:on
    ;

    private final String str;

    private Reason(String s) {
      str = s;
    }

    @Override
    public String toString() {
      return str;
    }
  }

  private static final long serialVersionUID = 1L;
  private final Path path;
  private final Reason reason;

  /**
   * An exception processing a file.
   *
   * @param path the path causing the exception
   * @param reason the reason for the exception
   */
  public ImpSortException(Path path, Reason reason) {
    super("file: " + path + "; reason: " + reason);
    this.path = path;
    this.reason = reason;
  }

  /**
   * The path that caused this exception.
   *
   * @return the path
   */
  public Path getPath() {
    return path;
  }

  /**
   * The reason for the exception.
   *
   * @return the reason code
   */
  public Reason getReason() {
    return reason;
  }

}
