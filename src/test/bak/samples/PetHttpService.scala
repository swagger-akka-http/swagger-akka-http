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
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.server.Directives._
import spray.json.DefaultJsonProtocol
import akka.stream.ActorMaterializer
import akka.actor.ActorSystem

@Api(value = "/pet", description = "Operations about pets.", produces = "application/json, application/vnd.test.pet", consumes = "application/json, application/vnd.test.pet")
@Path("/pet")
trait PetHttpService
    extends Directives
    with ModelFormats {

  implicit val actorSystem = ActorSystem("mysystem")
  implicit val materializer = ActorMaterializer()
  import actorSystem.dispatcher

  @ApiOperation(value = "Find a pet by ID", notes = "Returns a pet based on ID", httpMethod = "GET", response = classOf[Pet])
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "petId", value = "ID of pet that needs to be fetched", required = true, dataType = "integer", paramType = "path", allowableValues = "[1,100000]")))
  @ApiResponses(Array(
    new ApiResponse(code = 404, message = "Pet not found"),
    new ApiResponse(code = 400, message = "Invalid ID supplied")))
  def readRoute = get {
    path("/pet" / Segment) { id ⇒
      complete(id)
    }
  }

  @ApiOperation(value = "Updates a pet in the store with form data.", notes = "", nickname = "updatePetWithForm", httpMethod = "POST", consumes = "multipart/form-data")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "petId", value = "ID of pet that needs to be updated", required = true, dataType = "string", paramType = "path"),
    new ApiImplicitParam(name = "name", value = "Updated name of the pet.", required = false, dataType = "string", paramType = "form"),
    new ApiImplicitParam(name = "status", value = "Updated status of the pet.", required = false, dataType = "string", paramType = "form")))
  @ApiResponses(Array(
    new ApiResponse(code = 404, message = "Dictionary does not exist.")))
  def updateRoute = post {
    path("/pet" / Segment) { id ⇒
      {
        formFields('name, 'status) { (name, status) ⇒
          complete("ok")
        }
      }
    }
  }

  @ApiOperation(value = "Deletes a pet", nickname = "deletePet", httpMethod = "DELETE")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "petId", value = "Pet id to delete", required = true, paramType = "path")))
  @ApiResponses(Array(
    new ApiResponse(code = 400, message = "Invalid pet value")))
  def deleteRoute = delete { path("/pet" / Segment) { id ⇒ complete(id) } }

  @ApiOperation(value = "Add a new pet to the store", nickname = "addPet", httpMethod = "POST", consumes = "application/json, application/xml")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "body", value = "Pet object that needs to be added to the store", required = true, paramType = "body")))
  @ApiResponses(Array(
    new ApiResponse(code = 405, message = "Invalid input")))
  def addRoute = post { path("/pet" / Segment) { id ⇒ complete(id) } }

  @ApiOperation(value = "Searches for a pet", nickname = "searchPet", httpMethod = "GET", produces = "application/json, application/xml")
  def searchRoute = get { complete("") }
}

case class Pet(id: Int, name: String, birthDate: java.util.Date)