package com.gettyimages.spray.swagger

import spray.routing.HttpService
import com.wordnik.swagger.annotations.Api
import com.wordnik.swagger.annotations.ApiOperation
import com.wordnik.swagger.annotations.ApiParamsImplicit
import com.wordnik.swagger.annotations.ApiParamImplicit
import spray.httpx.Json4sSupport

@Api(value = "/dict", description = "This is a dictionary api.")
trait DictHttpService extends HttpService with Json4sSupport {
  
  var dict: Map[String,String] = Map[String,String]()
  
  @ApiOperation(value = "Add dictionary entry.", notes = "Will a new entry to the dictionary, indexed by key, with an optional expiration value.", httpMethod = "POST")
  @ApiParamsImplicit(Array(
    new ApiParamImplicit(name = "entry", value = "Key/Value pair of dictionary entry, with optional expiration time.", required = true, dataType = "DictEntry", paramType = "body")
  ))
  def createRoute = post { path("/dict") { entity(as[DictEntry]) { e =>
    dict += e.key -> e.value 
    complete("ok")
  }}}
  
  @ApiOperation(value = "Find entry by key.", notes = "Will look up the dictionary entry for the provided key.", responseClass = "DictEntry", httpMethod = "GET")
  @ApiParamsImplicit(Array(
    new ApiParamImplicit(name = "key", value = "Keyword for the dictionary entry.", required = true, dataType = "String", paramType = "path")
  ))
  def readRoute = get { path("/dict" / Segment) { key =>
    complete(dict(key))  
  }}

}
  
case class DictEntry(
  val key: String,
  val value: String,
  val expire: Option[Long]
)