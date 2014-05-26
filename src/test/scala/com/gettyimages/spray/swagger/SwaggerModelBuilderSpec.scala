/**
 * Copyright 2013 Getty Imges, Inc.
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

import scala.reflect.runtime.universe._
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.WordSpec
import com.wordnik.swagger.annotations.ApiModel
import ReflectionUtils._
import SwaggerModelBuilderSpecValues._
import org.scalatest.matchers.BePropertyMatcher
import scala.annotation.meta.field
import com.wordnik.swagger.annotations.ApiModelProperty
import org.joda.time.DateTime
import java.util.Date


class SwaggerModelBuilderSpec extends WordSpec with ShouldMatchers {

  implicit val mirror = runtimeMirror(getClass.getClassLoader)

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

        model.properties should have size 9
        checkProperty("name", NameDescription, "string")
        checkProperty("count", CountDescription, "int")
        checkProperty("isStale", IsStaleDescription, "boolean")
        checkProperty("offset", OffsetDescription, "int")
        checkProperty("nodes", NodesDescription, "array")
        checkProperty("enum", EnumDescription, "string")
        checkProperty("startDate", StartDateDescription, "dateTime")
        checkProperty("endDate", EndDateDescription, "dateTime")

        model.properties("enum").enum should be ('defined)
        val enumValues = model.properties("enum").enum.get
        enumValues should have size 2
        enumValues should contain ("a")
        enumValues should contain ("b")
        
        model.properties("allowable").enum should be ('defined)
        val allowableValues = model.properties("allowable").enum.get
        allowableValues should have size 2
        allowableValues should contain ("first")
        allowableValues should contain ("second")
        

        model.`extends` should be ('defined)
        model.`extends`.get should be ("TestModelParent")
      }
      "correctly process dataType in ApiModelProperty annotations" in {
        implicit val model = buildAndGetModel("ModelWithCustomPropertyDatatypes", typeOf[ModelWithCustomPropertyDatatypes])
        model.properties should have size (7)
        checkProperty("count", CountDescription, "long")
        checkProperty("isStale", IsStaleDescription, "boolean")
        checkProperty("offset", OffsetDescription, "array")
        checkProperty("endDate", EndDateDescription, "date")
        checkProperty("nonDefaultTypeField", NameDescription, "CustomType")
        checkProperty("nonDefaultContainerTypeField", NameDescription, "CustomContainer")
        checkProperty("amount", AmountDescription, "BigDecimal")
      }
    }
    "passed multiple test models" should {
      "build all of them" in {
        val builder = new SwaggerModelBuilder(Seq(typeOf[TestModelEmptyAnnotation], typeOf[TestModel], typeOf[TestModelNode]))
        val allModels = builder.buildAll
        allModels should have size 3
        allModels should contain key "TestModelEmptyAnnotation"
        allModels should contain key "TestModel"
        allModels should contain key "TestModelNode"
      }
    }
    "passed a model with subtypes" should {
      "have subtypes available" in {
        val builder = new SwaggerModelBuilder(Seq(typeOf[TestModelParent]))
        val parentModel = builder.build("TestModelParent")
        parentModel should be ('defined)
        parentModel.get.subTypes should be ('defined)
        parentModel.get.subTypes.get should have size 1
        parentModel.get.subTypes.get should contain ("TestModel")
      }
    }
    "passed a model with subtypes specified in the annotation" should {
      "have natural subtypes overriden by annotation specification" in {
         val builder = new SwaggerModelBuilder(Seq(typeOf[Letter]))
         val parentModel = builder.build("Letter")
         parentModel should be ('defined)
         parentModel.get.subTypes should be ('defined)
         parentModel.get.subTypes.get should have size 2
         parentModel.get.subTypes.get should contain ("String")
         parentModel.get.subTypes.get should contain ("B")
      }
    }
    "passed a model with explicit property positions" should {
      "output the properties in position order" in {
        implicit val model = buildAndGetModel("TestModelPositions", typeOf[TestModelPositions])
        model.properties.values.zip(model.properties.values.drop(1)) foreach { p =>
          p._1.position should be <= p._2.position
        }
      }
    }
  }

  private def checkProperty(modelKey: String, description: String, `type`: String)(implicit model: Model) {
    model.properties should contain key modelKey
    val prop = model.properties(modelKey)
    prop.description should equal (description)
    prop.`type` should equal (`type`)
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
  final val EnumDescription = "enumDescription2135432"
  final val StartDateDescription = "startDateDescription294290"
  final val EndDateDescription = "endDateDescription294290"
  final val AmountDescription = "amountDescription222"
  final val AllowableDescription = "allowableDesciption"
}

case class TestModelWithNoAnnotation()

@Deprecated
case class TestModelWithWrongAnnotation()

@ApiModel
case class TestModelEmptyAnnotation()

@ApiModel
sealed trait TestModelParent {

}

@ApiModel(description = TestModelDescription)
case class TestModel(
    @(ApiModelProperty @field)(value = NameDescription)
    name: String,
    @(ApiModelProperty @field)(value = CountDescription)
    count: Int,
    @(ApiModelProperty @field)(value = IsStaleDescription)
    isStale: Boolean,
    @(ApiModelProperty @field)(value = OffsetDescription)
    offset: Option[Int] = None,
    @(ApiModelProperty @field)(value = NodesDescription)
    nodes: List[TestModelNode] = List[TestModelNode](),
    @(ApiModelProperty @field)(value = EnumDescription)
    enum: TestEnum.TestEnum = TestEnum.AEnum,
    @(ApiModelProperty @field)(value = StartDateDescription)
    startDate: Date,
    @(ApiModelProperty @field)(value = EndDateDescription)
    endDate: DateTime,
    noAnnotationProperty: String,
    secondNoAnnotationProperty: String,
    @(ApiModelProperty @field)(value = AllowableDescription, allowableValues="first, second") allowable: String
) extends TestModelParent

@ApiModel(description = TestModelDescription)
case class ModelWithCustomPropertyDatatypes(
  @(ApiModelProperty @field)(value = CountDescription, dataType = "long")
  count: BigInt,
  @(ApiModelProperty @field)(value = IsStaleDescription, dataType = "boolean")
  isStale: Any,
  @(ApiModelProperty @field)(value = OffsetDescription, dataType = "array[int]")
  offset: Iterable[(Int, Boolean)],
  @(ApiModelProperty @field)(value = EndDateDescription, dataType = "date", required = false)
  endDate: Option[String],
  @(ApiModelProperty @field)(value = NameDescription, dataType = "CustomType", required = false)
  nonDefaultTypeField: Option[String],
  @(ApiModelProperty @field)(value = NameDescription, dataType = "CustomContainer[string]", required = false)
  nonDefaultContainerTypeField: Option[String],
  @(ApiModelProperty @field)(value = AmountDescription, dataType="BigDecimal")
  amount: BigDecimal
)


@ApiModel(description = "ModelBase")
class ModelBase {
  @(ApiModelProperty @field)(value = NameDescription)
  val name: String = ""
}

@ApiModel(description = "ModelExtension", parent = classOf[ModelBase])
class ModelExtension extends ModelBase {
  @(ApiModelProperty @field)(value = EndDateDescription)
  val date: Date = DateTime.now().toDate
}


object TestEnum extends Enumeration {
  type TestEnum = Value
  val AEnum = Value("a")
  val BEnum = Value("b")
}

@ApiModel
case class TestModelNode(
  value: Option[String]
)

case class A() extends Letter
case class B() extends Letter

@ApiModel(
  subTypes = Array(classOf[String], classOf[B])
)
abstract class Letter

@ApiModel
case class TestModelPositions(
  @(ApiModelProperty @field)(position = 0, value = "") arg0: String,
  @(ApiModelProperty @field)(position = 1, value = "") arg1: String,
  @(ApiModelProperty @field)(position = 2, value = "") arg2: String,
  @(ApiModelProperty @field)(position = 3, value = "") arg3: String
)
