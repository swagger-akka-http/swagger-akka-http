package com.gettyimages.spray.swagger

import scala.reflect.runtime.universe._

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.WordSpec
import com.wordnik.swagger.annotations.ApiClass

class SwaggerModelBuilderSpec extends WordSpec with ShouldMatchers  {

  "A test model" when {
    "passed to SwaggerModelBuilder" should {
      "throw an IllegalArgumentException if it has no annotation" in {
         intercept[IllegalArgumentException] {
           new SwaggerModelBuilder(List(typeOf[TestModelWithNoAnnotation]))
         } 
      }
      "throw an IllegalArgumentException if it has the wrong annotation" in {
        intercept[IllegalArgumentException] {
           new SwaggerModelBuilder(List(typeOf[TestModelWithWrongAnnotation]))
        }
      }
      "be buildable if it has an empty ApiClass annotation" in {
        val builder = new SwaggerModelBuilder(Seq(typeOf[TestModelEmptyAnnotation]))
        assert(builder.build("abcdef").isEmpty)
        val modelOpt = builder.build("TestModelEmptyAnnotation") 
        assert(modelOpt.isDefined)
        val model = modelOpt.get
        assert(model.id === "TestModelEmptyAnnotation")
        assert(model.description)
        assert(model.properties.isEmpty)
      }
      "be buildable if it has a description and property annotations" in {
        val builder = new SwaggerModelBuilder(Seq(typeOf[TestModel]))
        
      }
    }
  }
}

case class TestModelWithNoAnnotation

@Deprecated
case class TestModelWithWrongAnnotation

@ApiClass
case class TestModelEmptyAnnotation

@ApiClass(
  description = "hello world, goodbye!"
)
case class TestModel(
    val name: String,
    val count: Int,
    val isStale: Boolean,
    val offset: Option[Int] = None,
    val nodes: List[TestModelNode] = List[TestModelNode]()
)

case class TestModelNode(
  val value: Option[String]
)