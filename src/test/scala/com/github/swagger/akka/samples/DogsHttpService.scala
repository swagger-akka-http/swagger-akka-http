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

import javax.ws.rs.Path

import akka.actor.{ActorRefFactory, ActorSystem}
import akka.http.scaladsl.model.ContentTypes.`application/json`
import akka.http.scaladsl.model.HttpMethods.OPTIONS
import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.model.headers.`Access-Control-Allow-Methods`
import akka.http.scaladsl.model.{HttpEntity, HttpResponse}
import akka.http.scaladsl.server.{Directives, Route}
import com.github.swagger.akka._
import com.github.swagger.akka.model.{Contact, Info, License}
import io.swagger.annotations._

import scala.reflect.runtime.universe._

case class Dog(breed: String)

class NestedService(system: ActorSystem) {self =>
  val swaggerService = new SwaggerHttpService {
    override val apiTypes = Seq(typeOf[Dogs.type])
    override val host = "some.domain.com"
    override val basePath = "api-doc"
    override val unwantedDefinitions = Seq("Function1", "Function1RequestContextFutureRouteResult")

    override val info: Info = Info(
      description = "Dogs love APIs",
      version = "1.0",
      title = "Test API Service",
      termsOfService = "Lenient",
      contact = Some(Contact("Lassie", "http://lassie.com", "lassie@tvland.com")),
      license = Some(License("Apache", "http://license.apache.com")))

    implicit def actorRefFactory: ActorRefFactory = system
  }

  @Api(value="/dogs")
  @Path(value = "/dogs")
  object Dogs extends Directives {

    implicit def actorRefFactory: ActorRefFactory = self.system
    implicit val json4sFormats = org.json4s.DefaultFormats

    @ApiOperation(value="List all of the dogs",
      notes = "Dogs are identified by unique strings",
      response = classOf[ListReply[Dog]],
      httpMethod = "GET",
      nickname = "getDogs"
    )
    @ApiResponses(Array(
      new ApiResponse(code = 200,
        message = "OK"),
      new ApiResponse(code = 404, message = "Dog not found"),
      new ApiResponse(code = 500, message = "Internal Server Error")
    ))
    def getDogs = path("dogs"){
      complete("dogs")
    }

    @ApiOperation(value="Options for dogs",
      notes = "dog notes",
      response = classOf[Void],
      httpMethod = "OPTIONS"
    )
    @ApiResponses(Array(new ApiResponse(code = 200, message = "OK")))
    def optionsRoute: Route = (path("dogs") & options) {
      complete(HttpResponse(OK, entity = HttpEntity.empty(`application/json`),
        headers = List(`Access-Control-Allow-Methods`(OPTIONS))))
    }
  }
}