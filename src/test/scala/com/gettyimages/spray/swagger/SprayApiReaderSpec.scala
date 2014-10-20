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
import com.wordnik.swagger.core.{ SwaggerSpec, SwaggerContext }
import com.wordnik.swagger.config._
import scala.reflect.runtime.universe._

class SprayApiReaderSpec
    extends WordSpec
    with ShouldMatchers {

  val SWAGGER_VERSION = "1.2"
  val API_VERSION = "1.0"
  val BASE_PATH = "http://www.example-foo.com"

  val reader = new SprayApiReader()
  def readType(t: Type) = {
    reader.read("", SwaggerContext.loadClass(t.toString), SwaggerConfig(API_VERSION, SWAGGER_VERSION, BASE_PATH, ""))
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
      val apiListingOpt = readType(typeOf[DictHttpService])
      val apiListing = apiListingOpt.get

      "return an ApiListing" in {
        apiListingOpt should be('defined)
      }

      "set the swagger version" in {
        apiListing.swaggerVersion shouldBe SWAGGER_VERSION
      }

      "set the API version" in {
        apiListing.apiVersion shouldBe API_VERSION
      }

      "set the basePath as the full url from the config" in {
        apiListing.basePath shouldBe BASE_PATH
      }

      "set the resourcePath as the value of the annotation" in {
        apiListing.resourcePath shouldBe "/dict"
      }

      "set the description from the api annotation" in {
        apiListing.description shouldEqual Some("This is a dictionary api.")
      }

      "set the apis property based on the ApiOperation annotations" in {
        apiListing.apis should have size (2)
      }
      "set the apis path based on ApiOperation and param values" in {
        val apiPaths = apiListing.apis.map(_.path)
        apiPaths should contain("/dict")
        apiPaths should contain("/dict/{key}")
      }

      "api operations should have data from @ApiOperations" in {

        val api = apiListing.apis.filter(_.path == "/dict").head
        val operations = api.operations
        operations should have size (1)
        val operation = operations.head
        val responseMessages = operation.responseMessages
        responseMessages should have size (1)
        val responseMessage = responseMessages.head
        responseMessage.code should be(400)
        responseMessage.message should be("Client Error")
        val notes = operation.notes
        notes should equal("Will a new entry to the dictionary, indexed by key, with an optional expiration value.")
        operation.nickname should equal("createRoute")

        val readPath = apiListing.apis.filter(_.path == "/dict/{key}").head
        readPath.operations.head.nickname shouldEqual "someothername"
      }
    }
  }
}
