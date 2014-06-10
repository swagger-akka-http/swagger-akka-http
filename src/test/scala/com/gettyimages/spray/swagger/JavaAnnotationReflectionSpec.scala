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
import scala.reflect.runtime.universe._
import scala.annotation.meta.field
import ReflectionUtils._

class JavaAnnotationReflectionSpec extends WordSpec with ShouldMatchers {
  
  class TestClass
  
  @TestJavaAnnotation(
    booleanValue = true, stringValue = "hello", intValue = 10, 
    arrayValue = Array(new ArrayTestJavaAnnotation("good")),
    classValue = classOf[TestClass]
  )
  case class TestModel(
    @(TestJavaAnnotation @field)(
      booleanValue = false, stringValue = "world", intValue = -10,
      arrayValue = Array(new ArrayTestJavaAnnotation("bye")),
      classValue = classOf[TestClass]
    )
      val testValue: String
  )
  
  implicit val mirror = runtimeMirror(getClass.getClassLoader)
  
  "A case class" when {
    "it has a class level java annotation" should {
      
      val annotation = getClassAnnotation[TestModel, TestJavaAnnotation]()
      
      "be extractable by type parameter" in {
         assert(!annotation.isEmpty, "class annotation does not exist")
         assert(annotation.get.tpe =:= typeOf[TestJavaAnnotation])
      }
      "be extractable with class objects" in {
        val annotation = getClassAnnotation[TestJavaAnnotation](typeOf[TestModel])
         assert(!annotation.isEmpty, "class annotation does not exist")
         assert(annotation.get.tpe =:= typeOf[TestJavaAnnotation])
      }
      "have boolean properties be extractable" in {
        testAnnotationProperty(
         getBooleanJavaAnnotation("booleanValue", annotation.get),
         true
      )}
      "have string properties be extractable" in {
        testAnnotationProperty(
          getStringJavaAnnotation("stringValue", annotation.get),
          "hello"
      )}
      "have integer properties be extractable" in {
        testAnnotationProperty(
          getIntJavaAnnotation("intValue", annotation.get), 
          10
      )}
      /*"have array properties be extractable" in {
        testAnnotationProperty(
          getArrayJavaAnnotation("arrayValue", annotation.get),
          Array(getClassAnnotation[TestJavaAnnotation, ArrayTestJavaAnnotation].get)
        )
      } */
    }
    "it has a constructor level java annotation with @field" should {
      
      val annotation = getFieldAnnotation[TestModel, TestJavaAnnotation]("testValue")
      
      "be extractable" in {
         assert(!annotation.isEmpty, "declaration annotation does not exist")
         assert(annotation.get.tpe =:= typeOf[TestJavaAnnotation])
      }
      "be extractable with class objects" in {
        val annotation = getFieldAnnotation[TestJavaAnnotation](typeOf[TestModel])("testValue")
         assert(!annotation.isEmpty, "class annotation does not exist")
         assert(annotation.get.tpe =:= typeOf[TestJavaAnnotation])
      }
      "have boolean properties be extractable" in {         
        testAnnotationProperty(
         getBooleanJavaAnnotation("booleanValue", annotation.get),
         false
      )}
      "have string properties be extractable" in {
        testAnnotationProperty(
          getStringJavaAnnotation("stringValue", annotation.get),
          "world"
      )}
      "have integer properties be extractable" in {
        testAnnotationProperty(
          getIntJavaAnnotation("intValue", annotation.get), 
          -10
      )}
      "have class properties be extractable" in {
        testAnnotationProperty(
            getClassJavaAnnotation[TestClass]("classValue", annotation.get),
            classOf[TestClass]
        )
      }
    }
    
    def testAnnotationProperty[T: TypeTag](value: Option[T], actualValue: T): Unit = {
      assert(!value.isEmpty, "${typeOf[T]} value does not exist")
      assert(value.get === actualValue)
    }
  }

}
