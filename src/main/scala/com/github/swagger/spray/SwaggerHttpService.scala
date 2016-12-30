package com.github.swagger.spray

import scala.collection.JavaConverters._
import scala.reflect.runtime.universe.Type
import scala.util.control.NonFatal

import com.github.swagger.spray.model._
import io.swagger.jaxrs.Reader
import io.swagger.jaxrs.config.ReaderConfig
import io.swagger.models.{ExternalDocs, Scheme, Swagger}
import io.swagger.models.auth.SecuritySchemeDefinition
import io.swagger.util.Json
import org.slf4j.LoggerFactory
import spray.http.MediaTypes
import spray.routing.{HttpService, PathMatcher0, Route}

object SwaggerHttpService {

  val logger = LoggerFactory.getLogger(classOf[SwaggerHttpService])

  val readerConfig = new ReaderConfig {
    def getIgnoredRoutes: java.util.Collection[String] = List[String]().asJavaCollection
    def isScanAllResources: Boolean = false
  }

  def toJavaTypeSet(apiTypes: Seq[Type]): Set[Class[_]] = {
    apiTypes.map(t => getClassForType(t)).toSet
  }

  private lazy val mirror = scala.reflect.runtime.universe.runtimeMirror(getClass.getClassLoader)

  def getClassForType(t: Type): Class[_] = {
    mirror.runtimeClass(t.typeSymbol.asClass)
  }
}

/**
 * @author rleibman
 */
trait SwaggerHttpService extends HttpService {
  import SwaggerHttpService._
  val apiTypes: Seq[Type]
  val host: String = "localhost"
  val basePath: String = "/"
  val apiDocsPath: String = "api-docs"
  val info: Info = Info()
  val scheme: Scheme = Scheme.HTTP
  val securitySchemeDefinitions: Map[String, SecuritySchemeDefinition] = Map()
  val externalDocs: Option[ExternalDocs] = None

  def swaggerConfig: Swagger = {
    val modifiedPath = prependSlashIfNecessary(basePath)
    val swagger = new Swagger().basePath(modifiedPath).host(host).info(info).scheme(scheme)
    swagger.setSecurityDefinitions(securitySchemeDefinitions.asJava)
    externalDocs match {
      case Some(ed) => swagger.externalDocs(ed)
      case None => swagger
    }
  }

  def prependSlashIfNecessary(path: String): String  = if(path.startsWith("/")) path else s"/$path"
  def removeInitialSlashIfNecessary(path: String): String =
    if(path.startsWith("/")) removeInitialSlashIfNecessary(path.substring(1)) else path
  def splitOnSlash(path:String): PathMatcher0 = path.split("/").map(segmentStringToPathMatcher).reduceLeft(_ / _)

  def reader: Reader = new Reader(swaggerConfig, readerConfig)

  def generateSwaggerJson: String = {
    try {
      val swagger: Swagger = reader.read(toJavaTypeSet(apiTypes).asJava)
      Json.pretty().writeValueAsString(swagger)
    } catch {
      case NonFatal(t) => {
        logger.error("Issue with creating swagger.json", t)
        throw t
      }
    }
  }

  lazy val routes: Route = get {
    import MediaTypes._

    path(splitOnSlash(removeInitialSlashIfNecessary(apiDocsPath)) / "swagger.json") {
      respondWithMediaType(`application/json`) {
        complete(generateSwaggerJson)
      }
    }
  }

}
