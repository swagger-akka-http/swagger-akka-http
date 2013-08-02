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