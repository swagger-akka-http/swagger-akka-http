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
package com.github.swagger.akka

import io.swagger.converter.ModelConverters
import io.swagger.models.properties.{RefProperty, DateProperty, ArrayProperty, StringProperty}
import org.scalatest.{Matchers, WordSpec}
import scala.collection.JavaConverters._
import com.github.swagger.akka.samples._

class SwaggerModelConverterSpec
  extends WordSpec
  with Matchers {

  import SwaggerModelBuilderSpecValues._

  "The ModelConverter object" when {
    "passed an annotated scala class" should {
      "produce a Swagger Model" in {
          val schemas = ModelConverters.getInstance().readAll(classOf[ModelBase])
          val userSchema = schemas.get ("ModelBase")
          userSchema.getDescription() should equal (ModelBaseDescription)
          val name = userSchema.getProperties().get("name")
          name.isInstanceOf[StringProperty] should be (true)
          name.getDescription() should equal ("name123")
        }
      "include the base class details in the Swagger Model" in {
          val schemas = ModelConverters.getInstance().readAll(classOf[ModelExtension])
          val userSchema = schemas.get ("ModelExtension")
          userSchema.getDescription() should equal (ModelExtensionDescription)
          val name = userSchema.getProperties().get("name")
          name.getDescription() should equal (NameDescription)
          val date = userSchema.getProperties().get("date")
          date.getDescription() should equal (EndDateDescription)
        }
      //@ApiOperation position is deprecated and ignored in Swagger 1.5.X
	    "order fields by position in the Swagger Model" ignore {
        val schemas = ModelConverters.getInstance().readAll(classOf[TestModelPositions])
        val userSchema = schemas.get ("TestModelPositions")
        userSchema should not be (null)
        val props = userSchema.getProperties().asScala.toList.map((t) => t._2)
        props should have size (4)
        props.foreach (p => {
          val posIdx = p.getPosition()
          posIdx should not be (null)
        })
      }
      "determine sub-types for a model" in {
        val schemas = ModelConverters.getInstance().readAll(classOf[Letter])
        val letterSchema = schemas.get ("Letter")
        letterSchema should not be (null)
        val bSchema = schemas.get("B")
        bSchema should not be (null)
      }
      "produce a Swagger Model for a model that contains custom properties" in {
        val schemas = ModelConverters.getInstance().readAll(classOf[ModelWithCustomPropertyDatatypes])
        val userSchema = schemas.get ("ModelWithCustomPropertyDatatypes")
        userSchema.getDescription() should equal (TestModelDescription)
        val count = userSchema.getProperties().get("count")
        count.getDescription() should equal (CountDescription)
        count.getType() should equal ("integer")
        count.getFormat() should equal ("int64")
        val isStale = userSchema.getProperties().get("isStale")
        isStale.getType() should equal ("boolean")
        val offset = userSchema.getProperties().get("offset").asInstanceOf[ArrayProperty]
        offset.getType() should equal ("array")
        offset.getItems().getType() should equal ("integer")
        offset.getItems().getFormat() should equal ("int32")
        val date = userSchema.getProperties().get("endDate").asInstanceOf[DateProperty]
        date.getRequired() should be (false)
        date.getType() should be ("string")
        date.getFormat() should be ("date")
        val amount = userSchema.getProperties().get("amount")
        amount.getType() should be ("number")
        amount.getFormat() should be ("double")
      }
      "produce a properly typed array from a List[T]" in {
        val schemas = ModelConverters.getInstance().readAll(classOf[TestModel])
        val userSchema = schemas.get ("TestModel")
        val nodes = userSchema.getProperties().get("nodes")
        nodes.getType() should be ("array")
        val items = nodes.asInstanceOf[ArrayProperty].getItems().asInstanceOf[RefProperty]
        items.get$ref() should equal ("#/definitions/TestModelNode")
        items.getSimpleRef() should equal ("TestModelNode")
        val testModelNode = schemas.get("TestModelNode")
        val value = testModelNode.getProperties().get("value")
        // NOTE using stock swagger-scala-module at this point
        // will have this as an array instead of a string.
        // swagger-api/swagger-scala-module Pull Request #9 addresses this
        value.getType() should equal ("ref")
      }
    }
  }
}