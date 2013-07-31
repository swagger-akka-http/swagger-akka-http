package com.gettyimages.spray.swagger

import scala.reflect.runtime.universe._
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.WordSpec
import com.wordnik.swagger.annotations.ApiClass
import ReflectionUtils._
import SwaggerModelBuilderSpecValues._
import org.scalatest.matchers.BePropertyMatcher
import scala.annotation.meta.field
import com.wordnik.swagger.annotations.ApiProperty


class SwaggerModelBuilderSpec extends WordSpec with ShouldMatchers {
  "A SwaggerModelBuilder " when {
    "passed a test model" should {
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
        builder.build("abcdef") should be ('empty)
        val modelOpt = builder.build("TestModelEmptyAnnotation") 
        modelOpt should be ('defined)
        val model = modelOpt.get
        model.id should equal ("TestModelEmptyAnnotation")
        model.description should equal(None)
        model.properties should be ('empty)
      }
      "be buildable and has a description" in {
        val model = buildAndGetModel("TestModel", typeOf[TestModel], typeOf[TestModelNode])
        model.description should be ('defined)
        model.description.get should equal (TestModelDescription)
      }
      "has the correct ApiProperty annotations" in {
        implicit val model = buildAndGetModel("TestModel", typeOf[TestModel], typeOf[TestModelNode])
        model.properties should have size (5)
        checkProperty[String]("name", NameDescription)
        checkProperty[Int]("count", CountDescription)
        checkProperty[Boolean]("isStale", IsStaleDescription)
        checkProperty[Int]("offset", OffsetDescription)
        checkProperty[List[_]]("nodes", NodesDescription)
      }
    }
    "passed multiple test models" should {
      "build all of them" in {
        val builder = new SwaggerModelBuilder(Seq(typeOf[TestModelEmptyAnnotation], typeOf[TestModel], typeOf[TestModelNode]))
        val allModels = builder.buildAll
        allModels should have size (3)
        allModels should contain key ("TestModelEmptyAnnotation")
        allModels should contain key ("TestModel")
        allModels should contain key ("TestModelNode")
      } 
    }
  }
  
  private def checkProperty[T: TypeTag](modelKey: String, description: String)(implicit model: Model) {
    model.properties should contain key (modelKey)
    val prop = model.properties(modelKey)
    prop.description should equal (description)
    prop.`type` should equal (typeOf[T].typeSymbol.name.decoded.trim)
  }
  
  private def buildAndGetModel(modelName: String, modelTypes: Type*): Model = {
    val builder = new SwaggerModelBuilder(modelTypes.toSeq)
    val modelOpt = builder.build(modelName) 
    assert(modelOpt.isDefined)
        
    val model = modelOpt.get
    assert(model.id === modelName)
    model
  }
}

object SwaggerModelBuilderSpecValues {
  final val TestModelDescription = "hello world, goodbye!"
  final val NameDescription = "name123"
  final val CountDescription = "count3125"
  final val IsStaleDescription = "isStale9325"
  final val OffsetDescription = "offestDescription9034"
  final val NodesDescription = "nodesDescription9043"
}

case class TestModelWithNoAnnotation

@Deprecated
case class TestModelWithWrongAnnotation

@ApiClass
case class TestModelEmptyAnnotation

@ApiClass(description = TestModelDescription)
case class TestModel(
    @(ApiProperty @field)(value = NameDescription)
    val name: String,
    @(ApiProperty @field)(value = CountDescription)
    val count: Int,
    @(ApiProperty @field)(value = IsStaleDescription)
    val isStale: Boolean,
    @(ApiProperty @field)(value = OffsetDescription)
    val offset: Option[Int] = None,
    @(ApiProperty @field)(value = NodesDescription)
    val nodes: List[TestModelNode] = List[TestModelNode](),
    
    val noAnnotationProperty: String,
    val secondNoAnnotationProperty: String
)

@ApiClass
case class TestModelNode(
  val value: Option[String]
)