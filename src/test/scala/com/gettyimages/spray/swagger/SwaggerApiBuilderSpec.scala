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
import spray.routing.HttpService
import akka.actor.Actor
import scala.reflect.runtime.universe._
import com.wordnik.swagger.annotations.Api
import com.wordnik.swagger.annotations.ApiOperation
import com.wordnik.swagger.annotations.ApiImplicitParams
import com.wordnik.swagger.annotations.ApiImplicitParam
import javax.ws.rs.Path

class SwaggerApiBuilderSpec extends WordSpec with ShouldMatchers {
  
  val swaggerApi = new SwaggerApiBuilder("1.2", "1.0", "swagger-specs", _: Seq[Type], _: Seq[Type])
  
  "A SwaggerApiBuilder" when {
    "passed a test api" should {
      "throw an IllegalArgumentException if it has no annotation" in {
         intercept[IllegalArgumentException] {
           swaggerApi(List(typeOf[TestApiWithNoAnnotation]), List[Type]())
         } 
      }
      "throw an IllegalArgumentException if it has the wrong annotation" in {
        intercept[IllegalArgumentException] {
           swaggerApi(List(typeOf[TestApiWithWrongAnnotation]), List[Type]())
        }
      }
      "throw an IllegalArgumentException if it doesn't extend HttpService" in {
        intercept[IllegalArgumentException] {
           swaggerApi(List(typeOf[TestApiDoesNotExtendHttpService]), List[Type]())
        }
      }
      "handle a properly annotated HttpService" in {
        val (resourceListing, apiListings) = swaggerApi(List(typeOf[DictHttpService]), List[Type](typeOf[DictEntry])).buildAll
        apiListings should have size (1)
        val (apiListingName, apiListing) = apiListings.head
        apiListingName should equal ("/dict")
        apiListing.apis should have size (2)
        val apiPaths = apiListing.apis.map(_.path)
        apiPaths should contain ("/dict") 
        apiPaths should contain ("/dict/{key}")
        val dictKeyApi = apiListing.apis.find(_.path == "/dict/{key}").get
        dictKeyApi.operations should be ('defined) 
        val operations = dictKeyApi.operations.get
        operations should have size (1)
        val operation = operations.head
        operation.responseMessages should be ('defined)
        val responseMessages = operation.responseMessages.get
        responseMessages should have size (1)
        val responseMessage = responseMessages.head
        responseMessage.code should be (404)
        responseMessage.message should be ("Dictionary does not exist.")
      }
    }
    "passed a test api with data only" should {
       "output that data model" in {
         val api = swaggerApi(List(typeOf[TestApiWithOnlyDataType]), List(typeOf[TestModel], typeOf[TestModelNode]))
         val (_, apiListings) = api.buildAll
         apiListings should contain key ("/test")
         val apiListing = apiListings("/test")
         apiListing.models should be ('defined)
         apiListing.models.get should contain key ("TestModel")
        }
      }  
    }
    "passed a test api with a sub path with path parameters" should {
      "output api on that sub path and test parameters identified" in {
        val api = swaggerApi(List(typeOf[TestApiWithPathOperation]), List(typeOf[TestModel], typeOf[TestModelNode]))
        val (_, apiListings) = api.buildAll
        apiListings should contain key ("/test")
        val apiListing = apiListings("/test")
        val operations = apiListing.apis
        operations should have size (2)
        operations(0).path should be ("/test/sub/{someParam}/path/{anotherParam}")
        operations(1).path should be ("/test/other/sub/{someParam}/path/{anotherParam}")
      }
    }
}

abstract class TestApiWithNoAnnotation extends HttpService

@Deprecated
abstract class TestApiWithWrongAnnotation extends HttpService

@Api(value = "/test")
abstract class TestApiDoesNotExtendHttpService

@Api(value = "/test")
abstract class TestApiWithOnlyDataType extends HttpService {
  @ApiOperation(value = "testApiOperation", httpMethod = "GET")
  @ApiImplicitParams(Array(new ApiImplicitParam(name = "test", value = "test param", dataType = "TestModel", paramType = "query")))
  def testOperation 
}

@Api(value = "/test")
abstract class TestApiWithPathOperation extends HttpService {
  @Path("/sub/{someParam}/path/{anotherParam}")
  @ApiOperation(value = "subPathApiOperation", httpMethod = "GET")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "someParam", value = "some param", dataType = "TestModel", paramType = "path"),
    new ApiImplicitParam(name = "anotherParam", value = "another param", dataType = "TestModel", paramType = "path")
  ))
  def subPathOperation
  
  @Path("/other/sub/{someParam}/path/{anotherParam}")
  @ApiOperation(value = "otherSubPathApiOperation", httpMethod = "GET")
  def otherSubPathOperation
}