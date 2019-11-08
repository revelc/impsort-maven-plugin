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


    
// some comment
    
   

  // package comment
package net.revelc.code.imp;
// example prefix line 1
  /* here */ import   org.junit.Test ;  // some comment here

 /* example prefix line 2 */   import   static   org.junit.Assert.assertTrue;   

 import org .junit.experimental.categories.Category   ;  import java.lang.String;

/* some
 * multiline
 * comment
 */
 import java.lang.String;

 import java.lang.String; // end of line

 // more for testing
 // more again
   

 // and again
import java . lang . *; /* just for testing */
   // another one

import java.lang.String; // last



@Category(java.lang.String.class)
public class BasicPluginTests {

  @Test
  public void testTautology() throws Exception {
                          assertTrue(true);
  }

}
