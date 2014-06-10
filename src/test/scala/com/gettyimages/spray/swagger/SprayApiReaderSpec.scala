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
import com.wordnik.swagger.core.{SwaggerSpec, SwaggerContext}
import com.wordnik.swagger.config._
import scala.reflect.runtime.universe._

class SprayApiReaderSpec
  extends WordSpec
  with ShouldMatchers {

  val reader = new SprayApiReader()
  def readType(t: Type) = {
    reader.read("", SwaggerContext.loadClass(t.toString), ConfigFactory.config)
  }

  "The SprayApiReader object" when {
    "passed an api with no annotation" should {
      "throw an IllegalArgumentException" in {
        intercept[IllegalArgumentException] {
          readType(typeOf[TestApiWithNoAnnotation])
        }
       }
     }

      "passed a properly annoted HttpService" should {
        "build an apiListing with all annotations read." in {
          val apiListingOpt = readType(typeOf[DictHttpService])

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
          responseMessage.code should be (400)
          responseMessage.message should be ("Client Error")
          val notes = operation.notes
          notes should equal ("Will a new entry to the dictionary, indexed by key, with an optional expiration value.")
          }
        }
      }
  }
