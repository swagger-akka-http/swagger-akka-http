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

import scala.reflect.runtime.universe.Type

import org.json4s.DefaultFormats
import org.json4s.Formats

import com.typesafe.scalalogging.slf4j.LazyLogging

import spray.routing.{PathMatcher, HttpService, Route}
import com.wordnik.swagger.model._
import com.wordnik.swagger.core.util.JsonSerializer
import com.wordnik.swagger.config.SwaggerConfig
import com.wordnik.swagger.core.SwaggerSpec
import spray.http.MediaTypes.`application/json`
import spray.http.StatusCodes.NotFound

trait SwaggerHttpService
extends HttpService
with LazyLogging {

  def apiTypes: Seq[Type]
  def modelTypes: Seq[Type]

  def apiVersion: String
  def swaggerVersion: String = SwaggerSpec.version

  def basePath: String //url of api
  def docsPath: String = "/api-docs"
  def apiInfo: Option[ApiInfo] = None
  //def authorizations: Option[Map[String, Authorization]] = None


  private val api =
    new SwaggerApiBuilder(
      new SwaggerConfig(
            apiVersion,
            swaggerVersion,
            basePath,
            "", //api path, not used
            List(), //authorizations
            apiInfo), apiTypes, modelTypes)


  final def routes: Route =
    respondWithMediaType(`application/json`) {
      get {
        path(docsPath) {
          complete(toJsonString(api.getResourceListing()))
        } ~ (
            (for (
              (subPath, apiListing) <- api.listings)
            yield {
              path(docsPath / subPath.drop(1).split('/').map(
                segmentStringToPathMatcher(_))
                  .reduceLeft(_ / _)) {
                    complete(toJsonString(apiListing))
                  }
           }).reduceLeft(_ ~ _))
      }
    }

      def toJsonString(data: Any): String = {
        if (data.getClass.equals(classOf[String])) {
          data.asInstanceOf[String]
        } else {
          JsonSerializer.asJson(data.asInstanceOf[AnyRef])
        }

      }
    }
