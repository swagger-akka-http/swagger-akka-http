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

import io.swagger.annotations._
import javax.ws.rs.Path

abstract class TestApiWithNoAnnotation

@Api(value = "/test")
@Path("/test")
abstract class TestApiDoesNotExtendHttpService {
  @ApiOperation(value = "testApiOperation", httpMethod = "GET")
  def testOperation
}

@Api(value = "/test")
@Path("/test")
abstract class TestApiWithOnlyDataType {
  @ApiOperation(value = "testApiOperation", httpMethod = "GET")
  @ApiImplicitParams(Array(new ApiImplicitParam(name = "test",
    value = "test param",
    dataType = "com.github.swagger.akka.samples.TestModel",
    paramType = "body")))
  def testOperation
}

@Api(value = "/test")
@Path("/test")
abstract class TestApiWithPathOperation {
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
@Path(value = "/test")
abstract class TestApiWithParamsHierarchy {
  @Path("/paramHierarchyOperation")
  @ApiOperation(value = "paramHierarchyOperation", httpMethod = "GET", response = classOf[ModelExtension])
  def paramHierarchyOperation
}

// because the swagger output format has the operations listed under paths, the most sensible
// option for ordering seems to be to order them correctly within a particular path, and then
// order the paths by the lowest position of an operation they contain, hence why the expected
// order here (as indicated by `value`) doesn't match the position attributes
@Api(value = "/test")
@Path("/test")
abstract class TestApiWithOperationPositions {
  @Path("/path1")
  @ApiOperation(position = 3, value = "order3", httpMethod = "HEAD", response = classOf[ModelBase])
  def operation4
  @Path("/path0")
  @ApiOperation(position = 0, value = "order0", httpMethod = "HEAD", response = classOf[ModelBase])
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

@Api(value = "/test")
@Path("/test")
abstract class TestApiWithResponseContainer {
  @ApiOperation(value = "testApiOperation",
    httpMethod = "GET",
    response = classOf[ListReply[TestModel]],
    responseContainer = "com.github.swagger.akka.samples.ListReply")
  def testOperation
}

@Api(value = "/test")
@Path("/test")
abstract class TestApiWithDateTime {
  @ApiOperation(value = "testApiOperation", httpMethod = "GET")
  @ApiImplicitParams(Array(new ApiImplicitParam(name = "test",
    value = "test param",
    dataType = "dateTime",
    paramType = "body")))
  def testOperation
}

@Api(value = "/test")
@Path("/test")
abstract class TestApiWithApiResponse {
  @ApiOperation(value = "testApiOperation",
    httpMethod = "GET",
    // code = 201,
    response = classOf[Pet])
  @ApiImplicitParams(Array(new ApiImplicitParam(name = "test",
    value = "test param",
    dataType = "dateTime",
    paramType = "body"
  )))
  @ApiResponses(Array(
    new ApiResponse(code = 1,
      message = "Successful",
      response = classOf[Pet]),
    new ApiResponse(code = 500,
      message = "Internal Server Error")
  ))
  def testOperation
}

@Api(value = "/test")
@Path("/test")
object TestApiWithObject {

}