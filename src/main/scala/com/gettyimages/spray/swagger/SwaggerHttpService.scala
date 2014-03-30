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

import com.typesafe.scalalogging.slf4j.Logging

import spray.httpx.Json4sSupport
import spray.routing.Directive.pimpApply
import spray.routing.{PathMatcher, HttpService, Route}

trait SwaggerHttpService extends HttpService with Logging with Json4sSupport {

  def apiTypes: Seq[Type]
  def modelTypes: Seq[Type]

  def apiVersion: String
  def swaggerVersion: String

  def baseUrl: String
  def specPath: String
  def resourcePath: String

  def apiInfo: Option[ApiInfo] = None
  def authorizations: Option[Map[String, Authorization]] = None

  implicit def json4sFormats: Formats = DefaultFormats

  private lazy val (resourceListing, apiListingMap) =
    (new SwaggerApiBuilder(
        swaggerVersion, apiVersion, baseUrl, apiTypes, modelTypes,
        apiInfo = SwaggerHttpService.this.apiInfo, authorizations = SwaggerHttpService.this.authorizations
    )).buildAll

  final def routes: Route = get { pathPrefix(specPath) {
    path(resourcePath) {
      complete(resourceListing)
    } ~ (for((apiPath, apiListing) <- apiListingMap) yield {
      path(resourcePath / apiPath.drop(1).split('/').map(segmentStringToPathMatcher(_)).reduceLeft(_ / _)) { complete(apiListing) }
    }).reduceLeft(_ ~ _)
  }}
}
