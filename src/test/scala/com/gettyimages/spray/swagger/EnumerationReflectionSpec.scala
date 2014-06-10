/**
 * Copyright 2014 Getty Imges, Inc.
 *
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
package com.gettyimages.spray.swagger

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.WordSpec
import ReflectionUtils._

import scala.reflect.runtime.universe._

class EnumerationReflectionSpec extends WordSpec with ShouldMatchers {
   "An enum" when {
     "it's inspected via Parameterized methods" should {
       "return all value symbols" in {
         val symbols = valueSymbols[TestEnum.type].toList
         symbols should have size (2)
         val symbolValues = symbols.map(_.name.decoded.trim)
         symbolValues should contain ("AEnum")
         symbolValues should contain ("BEnum")
       }
     }
     "it's inspected via type methods" should {
       "return all value symbols" in {
         val symbols = valueSymbols(typeOf[TestEnum.TestEnum]).toList
         symbols should have size (2)
         val symbolValues = symbols.map(_.name.decoded.trim)
         symbolValues should contain ("AEnum")
         symbolValues should contain ("BEnum")
       }
     }
   } 

}
