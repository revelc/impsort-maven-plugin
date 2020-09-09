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

import java.io.IOException;
import java.nio.file.Path;

/**
 * Signals that the file denoted by this path has an unknown line ending.
 *
 * <p>
 * This exception will be thrown by the {@link ImpSort#parseFile} when it encounters a file with an
 * unknown line ending.
 */
public class UnknownLineEndingException extends IOException {
  private static final long serialVersionUID = 1L;

  private final Path path;

  /**
   * Constructs a {@code UnknownLineEndingException} with the specified path.
   *
   * @param path the path
   */
  public UnknownLineEndingException(final Path path) {
    super("Unknown line ending for file " + path);
    this.path = path;
  }

  /**
   * Returns the path.
   *
   * @return the path
   */
  public Path getPath() {
    return path;
  }
}
