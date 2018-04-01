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

import javax.ws.rs.{Consumes, Path, Produces}
import akka.http.scaladsl.server.Directives
import akka.stream.ActorMaterializer
import akka.actor.ActorSystem
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import io.swagger.v3.oas.annotations.media.{Content, Schema}
import io.swagger.v3.oas.annotations.responses.ApiResponse

@Path("/pet")
@Consumes(value = Array("application/json", "application/vnd.test.pet"))
@Produces(value = Array("application/json", "application/vnd.test.pet"))
trait PetHttpService
  extends Directives
    with ModelFormats {

  implicit val actorSystem = ActorSystem("mysystem")
  implicit val materializer = ActorMaterializer()

  @Operation(summary = "Find a pet by ID",
    description = "Returns a pet based on ID",
    method = "GET",
    parameters = Array(
      new Parameter(name = "petId", in = ParameterIn.PATH, required = true, description = "ID of pet that needs to be fetched",
        content = Array(new Content(schema = new Schema(implementation = classOf[Int], allowableValues = Array("[1,100000]")))))
    ),
    responses = Array(
      new ApiResponse(responseCode = "400", description = "Invalid ID supplied"),
      new ApiResponse(responseCode = "404", description = "Pet not found")
    )
  )
  def readRoute = get {
    path("/pet" / Segment) { id ⇒
      complete(id)
    }
  }

  @Operation(summary = "updatePetWithForm",
    description = "Updates a pet in the store with form data.",
    method = "POST",
    parameters = Array(
      new Parameter(name = "petId", in = ParameterIn.PATH, required = true, description = "ID of pet that needs to be updated"),
      new Parameter(name = "name", in = ParameterIn.QUERY, required = true, description = "Updated name of the pet."),
      new Parameter(name = "status", in = ParameterIn.QUERY, required = true, description = "Updated status of the pet.")
    ),
    responses = Array(new ApiResponse(responseCode = "404", description = "Pet does not exist."))
  )
  def updateRoute = post {
    path("/pet" / Segment) { id ⇒
      formFields('name, 'status) { (name, status) ⇒
        complete("ok")
      }
    }
  }

  @Operation(summary = "deletePet",
    description = "Deletes a pet",
    method = "DELETE",
    parameters = Array(new Parameter(name = "petId", in = ParameterIn.PATH, required = true,
      description = "Pet id to delete")),
    responses = Array(new ApiResponse(responseCode = "400", description = "Invalid pet value"))
  )
  def deleteRoute = delete { path("/pet" / Segment) { id ⇒ complete(id) } }

//  @ApiImplicitParams(Array(
//    new ApiImplicitParam(name = "body", value = "Pet object that needs to be added to the store", required = true, paramType = "body")))
  @Consumes(value = Array("application/json", "application/xml"))
  @Operation(summary = "addPet",
    description = "Add a new pet to the store",
    method = "POST",
    parameters = Array(new Parameter(name = "petId", in = ParameterIn.PATH, required = true,
      description = "Pet id")),
    responses = Array(new ApiResponse(responseCode = "405", description = "Invalid input"))
  )
  def addRoute = post { path("/pet" / Segment) { id ⇒ complete(id) } }

  @Produces(value = Array("application/json", "application/xml"))
  @Operation(summary = "searchPet",
    description = "Searches for a pet",
    method = "GET")
  def searchRoute = get { complete("") }
}

case class Pet(id: Int, name: String, birthDate: java.util.Date)