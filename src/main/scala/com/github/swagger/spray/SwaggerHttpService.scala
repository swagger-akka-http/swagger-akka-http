package com.github.swagger.spray

import scala.collection.JavaConverters._
import scala.reflect.runtime.universe.Type

import com.github.swagger.spray.model._
import io.swagger.jaxrs.Reader
import io.swagger.jaxrs.config.ReaderConfig
import io.swagger.models.{ExternalDocs, Scheme, Swagger}
import io.swagger.models.auth.SecuritySchemeDefinition
import io.swagger.util.Json
import spray.http.MediaTypes
import spray.routing.{HttpService, PathMatcher0, Route}

object SwaggerHttpService {
  val readerConfig = new ReaderConfig {
    def getIgnoredRoutes(): java.util.Collection[String] = List[String]().asJavaCollection
    def isScanAllResources(): Boolean = false
  }

  def toJavaTypeSet(apiTypes: Seq[Type]): Set[Class[_]] = {
    apiTypes.map(t => Class.forName(getClassNameForType(t))).toSet
  }

  def getClassNameForType(t: Type): String = {
    def canFindClass(className: String): Boolean = {
      try {
        Class.forName(className) != null
      } catch {
        case t: Throwable => false
      }
    }
    val typeSymbol = t.typeSymbol
    val fullName = typeSymbol.fullName
    if (typeSymbol.isModuleClass) {
      val idx = fullName.lastIndexOf('.')
      if (idx >= 0) {
        val mangledName = s"${fullName.slice(0, idx)}.${fullName.slice(idx+1, fullName.length)}$$"
        if(canFindClass(mangledName)) {
          mangledName
        } else {
          s"${fullName.slice(0, idx)}$$${fullName.slice(idx+1,fullName.size)}$$"
        }
      } else {
        fullName
      }
    } else {
      fullName
    }
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
  def swagger: Swagger = reader.read(toJavaTypeSet(apiTypes).asJava)

  def toJsonString(s: Swagger): String = Json.mapper().writeValueAsString(s)

  lazy val routes: Route = get {
    import MediaTypes._

    path(splitOnSlash(removeInitialSlashIfNecessary(apiDocsPath)) / "swagger.json") {
      respondWithMediaType(`application/json`) {
        complete(toJsonString(swagger))
      }
    }
  }

}
