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

import com.wordnik.swagger.annotations.ApiModelProperty
import com.wordnik.swagger.annotations.ApiModel
import scala.annotation.meta.field
import org.joda.time.DateTime
import java.util.Date

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

@ApiModel(description = "an entry in the dictionary")
case class DictEntry(
  val key: String,
  val value: String,
  val expire: Option[Long]
  )


import SwaggerModelBuilderSpecValues._

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
