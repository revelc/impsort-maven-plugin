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

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.github.javaparser.JavaToken;
import com.github.javaparser.Position;
import com.github.javaparser.Problem;
import com.github.javaparser.TokenRange;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.TypeDeclaration;

public class ParseProblemFilter {

  /**
   * Only return problems before the first top level decleration. All other problems might for
   * example be caused be a newer Java version but should have no impact on import sorting.
   */
  public static List<Problem> getImportRelevantProblems(CompilationUnit unit,
      List<Problem> problems) {
    Optional<Position> firstTopLevelBegin = unit.getTypes().stream().map(TypeDeclaration::getBegin)
        .filter(Optional::isPresent).map(Optional::get).sorted().findFirst();
    if (firstTopLevelBegin.isEmpty()) {
      // should not happen but let's simply give up and return the problems as they are
      return problems;
    } else {
      return problems.stream()
          .filter(p -> p.getLocation().map(TokenRange::getBegin).flatMap(JavaToken::getRange)
              .map(r -> r.isBefore(firstTopLevelBegin.orElseThrow())).orElse(Boolean.FALSE))
          .collect(Collectors.toList());
    }
  }
}
