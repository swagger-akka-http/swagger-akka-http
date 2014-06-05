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

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.WordSpec
import com.wordnik.swagger.core.{SwaggerSpec, SwaggerContext}
import com.wordnik.swagger.config._
import scala.reflect.runtime.universe._

class SprayApiReaderSpec
  extends WordSpec
  with ShouldMatchers {

  val exampleApi = SwaggerContext.loadClass(typeOf[DictHttpService].toString)

  "The SprayApiReader object" when {
    "when reading classes" should {
      "handles a properly annotated HttpService" in {
        val reader = new SprayApiReader()
        val apiListingOpt = reader.read("", exampleApi, ConfigFactory.config)

        apiListingOpt should be ('defined)
        val apiListing = apiListingOpt.get
    /*    case class ApiListing (
          apiVersion: String,
          swaggerVersion: String,
          basePath: String,
          resourcePath: String,
          produces: List[String] = List.empty,
          consumes: List[String] = List.empty,
          protocols: List[String] = List.empty,
          authorizations: List[String] = List.empty,
          apis: List[ApiDescription] = List(),
          models: Option[Map[String, Model]] = None,
          description: Option[String] = None,
          position: Int = 0)
*/
        apiListing.resourcePath should equal ("/dict")
        apiListing.description shouldEqual Some("This is a dictionary api.")
        apiListing.apis should have size (2)
        val apiPaths = apiListing.apis.map(_.path)
        apiPaths should contain ("/dict")
        apiPaths should contain ("/dict/{key}")

        val api = apiListing.apis(1)
        val operations = api.operations
        operations should have size (1)
        val operation = operations.head
        val responseMessages = operation.responseMessages
        responseMessages should have size (1)
        val responseMessage = responseMessages.head
        responseMessage.code should be (404)
        responseMessage.message should be ("Dictionary does not exist.")
        val notes = operation.notes
        notes should equal ("Will look up the dictionary entry for the provided key.")
        }
      }
    }
  }
