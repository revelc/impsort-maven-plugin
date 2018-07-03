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

package net.revelc.code.imp;

import com.foo.Type1;
import com.foo.Type2;
import com.foo.Type3;
import com.foo.Type4;
import com.foo.Type5;
import com.foo.Type6;
import com.foo.Type7;
import com.foo.Type8;
import com.foo.Type9;
import com.foo.Type10;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableMap;
import io.swagger.annotations.ApiOperation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import static org.junit.Assert.*;

/**
 * The import of {@link HashMap} should not be stripped away, since it
 * is used in this comment.
 */
@Component
public class UnusedImports {
    ImmutableMap immutable;

    /**
     * The following should also not be removed:
     *
     * @see Map
     */
    public List<Integer> getList() {
        assertFalse(false);
        return null;
    }

	/**
	 * {@link Type1#method()}
	 * {@link Type2#method(Type3, Type4)}
	 * {@link #method(Type5, Type6)}
	 * {@value Type7#field}
	 * @see Type8#method()
	 * @see Type9#method(Type10)
	 */
	public void foo() {
	}
}
