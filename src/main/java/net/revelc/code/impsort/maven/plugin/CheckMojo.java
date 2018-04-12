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

import java.nio.file.Path;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import net.revelc.code.impsort.Grouper;

@Mojo(name = "check", defaultPhase = LifecyclePhase.PROCESS_SOURCES, threadSafe = true, requiresDependencyResolution = ResolutionScope.NONE)
public class CheckMojo extends AbstractImpSortMojo {

  @Override
  public void processResult(Path path, Grouper.Result results) throws MojoFailureException {
    if (!results.isSorted()) {
      fail("Imports are not sorted in " + path);
    }
  }

}
