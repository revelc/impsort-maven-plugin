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

import com.github.javaparser.*;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.TypeDeclaration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ParseProblemFilter {

  /**
   * Only return problems above the first top level declaration (if any, otherwise return all). All
   * other problems might for example be caused be a newer Java version with not yet known syntax
   * but should have no impact on import parsing.
   */
  public static List<Problem> getProblemsAboveFirstTopLevelDeclaration(CompilationUnit unit,
      List<Problem> problems) {
    Optional<Position> firstTopLevelBegin = unit.getTypes().stream().map(TypeDeclaration::getBegin)
        .filter(Optional::isPresent).map(Optional::get).sorted().findFirst();
    // in case of no top level declaration per definition all problems are above ;-)
    if (firstTopLevelBegin.isEmpty()) {
      return problems;
    }

    return problems.stream()
        .filter(problem -> toRange(problem)
            .map(problemRange -> problemRange.isBefore(firstTopLevelBegin.orElseThrow()))
            .orElse(Boolean.TRUE))
        .collect(Collectors.toList());
  }

  public static Optional<Range> toRange(Problem problem) {
    return problem.getLocation().map(TokenRange::getBegin).flatMap(JavaToken::getRange);
  }
}
