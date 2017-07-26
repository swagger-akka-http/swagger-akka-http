/*
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
package com.github.swagger.akka

import scala.collection.JavaConverters._
import scala.reflect.runtime.universe.Type
import scala.util.control.NonFatal
import com.github.swagger.akka.model.Info
import com.github.swagger.akka.model.scala2swagger
import akka.http.scaladsl.model.{HttpEntity, MediaTypes}
import akka.http.scaladsl.server.{Directives, PathMatchers, Route}
import io.swagger.jaxrs.Reader
import io.swagger.jaxrs.config.DefaultReaderConfig
import io.swagger.models.{ExternalDocs, Scheme, Swagger}
import io.swagger.models.auth.SecuritySchemeDefinition
import io.swagger.util.{Json, Yaml}
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory

object SwaggerHttpService {

  val logger = LoggerFactory.getLogger(classOf[SwaggerHttpService])
  val readerConfig = new DefaultReaderConfig

  def toJavaTypeSet(apiTypes: Seq[Type]): Set[Class[_]] = {
    apiTypes.map(t => getClassForType(t)).toSet
  }

  private lazy val mirror = scala.reflect.runtime.universe.runtimeMirror(getClass.getClassLoader)

  def getClassForType(t: Type): Class[_] = {
    mirror.runtimeClass(t.typeSymbol.asClass)
  }

  def removeInitialSlashIfNecessary(path: String): String =
    if(path.startsWith("/")) removeInitialSlashIfNecessary(path.substring(1)) else path
}

trait SwaggerHttpService extends Directives {

  import SwaggerHttpService._
  val apiTypes: Seq[Type]
  val host: String = ""
  val basePath: String = "/"
  val apiDocsPath: String = "api-docs"
  val info: Info = Info()
  val scheme: Scheme = Scheme.HTTP
  val securitySchemeDefinitions: Map[String, SecuritySchemeDefinition] = Map()
  val externalDocs: Option[ExternalDocs] = None
  val vendorExtensions: Map[String, Object] = Map.empty
  val unwantedDefinitions: Seq[String] = Seq.empty

  def swaggerConfig: Swagger = {
    val modifiedPath = prependSlashIfNecessary(basePath)
    val swagger = new Swagger().basePath(modifiedPath).info(info).scheme(scheme)
    if(StringUtils.isNotBlank(host)) swagger.host(host)
    swagger.setSecurityDefinitions(securitySchemeDefinitions.asJava)
    externalDocs match {
      case Some(ed) => swagger.externalDocs(ed)
      case None => swagger
    }
    swagger.vendorExtensions(vendorExtensions.asJava)
  }

  def reader = new Reader(swaggerConfig, readerConfig)
  def prependSlashIfNecessary(path: String): String  = if(path.startsWith("/")) path else s"/$path"

  def generateSwaggerJson: String = {
    try {
      Json.pretty().writeValueAsString(filteredSwagger)
    } catch {
      case NonFatal(t) => {
        logger.error("Issue with creating swagger.json", t)
        throw t
      }
    }
  }

  def generateSwaggerYaml: String = {
    try {
      Yaml.pretty().writeValueAsString(filteredSwagger)
    } catch {
      case NonFatal(t) => {
        logger.error("Issue with creating swagger.yaml", t)
        throw t
      }
    }
  }

  private def filteredSwagger: Swagger = {
    val swagger: Swagger = reader.read(toJavaTypeSet(apiTypes).asJava)
    swagger.setDefinitions(swagger.getDefinitions.asScala.filterKeys(definitionName => !unwantedDefinitions.contains(definitionName)).asJava)
    swagger
  }

  lazy val apiDocsBase = PathMatchers.separateOnSlashes(removeInitialSlashIfNecessary(apiDocsPath))

  lazy val routes: Route =
    path(apiDocsBase / "swagger.json") {
      get {
        complete(HttpEntity(MediaTypes.`application/json`, generateSwaggerJson))
      }
    } ~
    path(apiDocsBase / "swagger.yaml") {
      get {
        complete(HttpEntity(CustomMediaTypes.`text/vnd.yaml`, generateSwaggerYaml))
      }
    }
}
