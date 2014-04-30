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
        val notes = operation.notes.get
        notes should equal ("Will look up the dictionary entry for the provided key.")
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
    "passed a test api with a method returning complex entity" should {
      "respect model class hierarchy" in {
        val api = swaggerApi(List(typeOf[TestApiWithParamsHierarchy]), List(typeOf[ModelBase], typeOf[ModelExtension]))
        val (_, apiListings) = api.buildAll
        apiListings should contain key ("/test")
        val apiListing = apiListings("/test")
        val operations = apiListing.apis
        operations should have size (1)
        operations(0).path should be ("/test/paramHierarchyOperation")
        val model = apiListing.models.get("ModelExtension")
        model.properties("date").`type` should be ("dateTime")
        model.properties("name").`type` should be ("string")
      }
    }
    "passed a test api with explicit operation positions" should {
      "output the operations in position order" in {
        val api = swaggerApi(List(typeOf[TestApiWithOperationPositions]), List(typeOf[ModelBase], typeOf[ModelExtension]))
        val (_, apiListings) = api.buildAll
        apiListings should contain key ("/test")
        val apiListing = apiListings("/test")
        val operations = apiListing.apis
        operations should have size 3
        operations(0).operations.get.apply(0).summary should be ("order0")
        operations(0).operations.get.apply(1).summary should be ("order1")
        operations(1).operations.get.apply(0).summary should be ("order2")
        operations(1).operations.get.apply(1).summary should be ("order3")
        operations(2).operations.get.apply(0).summary should be ("order4")
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
  @ApiOperation(value = "subPathApiOperation", httpMethod = "GET", notes = "some notes")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "someParam", value = "some param", dataType = "TestModel", paramType = "path"),
    new ApiImplicitParam(name = "anotherParam", value = "another param", dataType = "TestModel", paramType = "path")
  ))
  def subPathOperation

  @Path("/other/sub/{someParam}/path/{anotherParam}")
  @ApiOperation(value = "otherSubPathApiOperation", httpMethod = "GET")
  def otherSubPathOperation
}

@Api(value = "/test")
abstract class TestApiWithParamsHierarchy extends HttpService {
  @Path("/paramHierarchyOperation")
  @ApiOperation(value = "paramHierarchyOperation", httpMethod = "GET", response = classOf[ModelBase])
  def paramHierarchyOperation
}

// because the swagger output format has the operations listed under paths, the most sensible
// option for ordering seems to be to order them correctly within a particular path, and then
// order the paths by the lowest position of an operation they contain, hence why the expected
// order here (as indicated by `value`) doesn't match the position attributes
@Api(value = "/test")
abstract class TestApiWithOperationPositions extends HttpService {
  @Path("/path1")
  @ApiOperation(position = 3, value = "order3", httpMethod = "GET", response = classOf[ModelBase])
  def operation4
  @Path("/path0")
  @ApiOperation(position = 0, value = "order0", httpMethod = "GET", response = classOf[ModelBase])
  def operation0
  @Path("/path1")
  @ApiOperation(position = 1, value = "order2", httpMethod = "GET", response = classOf[ModelBase])
  def operation1
  @Path("/path0")
  @ApiOperation(position = 2, value = "order1", httpMethod = "GET", response = classOf[ModelBase])
  def operation3
  @Path("/path2")
  @ApiOperation(position = 4, value = "order4", httpMethod = "GET", response = classOf[ModelBase])
  def operation2
}
