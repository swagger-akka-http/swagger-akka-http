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

import com.wordnik.swagger.annotations._
import javax.ws.rs.Path
import spray.routing.HttpService
import spray.httpx.Json4sSupport

@Api(value = "/dict", description = "This is a dictionary api.")
trait DictHttpService extends HttpService with Json4sSupport {

  var dict: Map[String,String] = Map[String,String]()

  @ApiOperation(value = "Add dictionary entry.", notes = "Will a new entry to the dictionary, indexed by key, with an optional expiration value.", httpMethod = "POST")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "entry", value = "Key/Value pair of dictionary entry, with optional expiration time.", required = true, dataType = "DictEntry", paramType = "body")
  ))
  def createRoute = post { path("/dict") { entity(as[DictEntry]) { e =>
    dict += e.key -> e.value
    complete("ok")
  }}}

  @ApiOperation(value = "Find entry by key.", notes = "Will look up the dictionary entry for the provided key.", response = classOf[DictEntry], httpMethod = "GET")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "key", value = "Keyword for the dictionary entry.", required = true, dataType = "String", paramType = "path")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 404, message = "Dictionary does not exist.")
  ))
  def readRoute = get { path("/dict" / Segment) { key =>
    complete(dict(key))
  }}

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
