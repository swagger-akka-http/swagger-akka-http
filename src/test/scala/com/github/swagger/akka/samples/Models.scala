/**
  * Copyright 2014 Getty Images, Inc.
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
package com.github.swagger.akka.samples

import io.swagger.v3.oas.annotations.media.Schema

object SwaggerModelBuilderSpecValues {
  final val TestModelDescription = "hello world, goodbye!"
  final val ModelBaseDescription = "description for ModelBase"
  final val ModelExtensionDescription = "description for ModelExtension"
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

@Schema(description = "an entry in the dictionary")
case class DictEntry(
                      val key: String,
                      val value: String,
                      val expire: Option[Long]
                    )

case class TestModelWithNoAnnotation()

@Deprecated
case class TestModelWithWrongAnnotation()

@Schema
case class TestModelEmptyAnnotation()

@Schema
sealed trait TestModelParent {

}
