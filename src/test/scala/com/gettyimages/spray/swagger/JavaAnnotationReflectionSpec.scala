package com.gettyimages.spray.swagger

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.WordSpec
import scala.reflect.runtime.universe._
import scala.annotation.meta.field
import ReflectionUtils._

class JavaAnnotationReflectionSpec extends WordSpec with ShouldMatchers {
  
  @TestJavaAnnotation(
    booleanValue = true, stringValue = "hello", intValue = 10
  )
  case class TestModel(
      @(TestJavaAnnotation @field)(
        booleanValue = false, stringValue = "world", intValue = -10
      )
      val testValue: String
  )
  
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
    }
    
    def testAnnotationProperty[T: TypeTag](value: Option[T], actualValue: T): Unit = {
      assert(!value.isEmpty, "${typeOf[T]} value does not exist")
      assert(value.get === actualValue)
    }
  }

}