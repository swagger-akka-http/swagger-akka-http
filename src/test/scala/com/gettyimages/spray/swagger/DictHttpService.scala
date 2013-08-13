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

import spray.routing.HttpService
import com.wordnik.swagger.annotations.Api
import com.wordnik.swagger.annotations.ApiOperation
import com.wordnik.swagger.annotations.ApiImplicitParams
import com.wordnik.swagger.annotations.ApiImplicitParam
import spray.httpx.Json4sSupport
import com.wordnik.swagger.annotations.ApiResponses
import com.wordnik.swagger.annotations.ApiResponse
import com.wordnik.swagger.annotations.ApiModel

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
 
@ApiModel(description = "an entry in the dictionary")
case class DictEntry(
  val key: String,
  val value: String,
  val expire: Option[Long]
)