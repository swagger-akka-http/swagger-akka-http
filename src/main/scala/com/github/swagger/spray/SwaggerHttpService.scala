package com.github.swagger.spray

import scala.collection.JavaConversions._
import com.github.swagger.spray.model._

import io.swagger.jaxrs.Reader
import io.swagger.jaxrs.config.ReaderConfig
import io.swagger.models.{Scheme, Swagger}
import io.swagger.util.Json

import spray.http.MediaTypes
import spray.json.{JsObject, pimpString}
import spray.routing.{ HttpService, Route }

import scala.reflect.runtime.universe.Type

/**
 * @author rleibman
 */
trait SwaggerHttpService extends HttpService {
  val apiTypes: Seq[Type]
  val host: String = "localhost"
  val basePath = "api-docs"
  val info: Info = Info()
  val scheme: Scheme = Scheme.HTTP
  val readerConfig = new ReaderConfig {
    def getIgnoredRoutes(): java.util.Collection[String] = List()
    def isScanAllResources(): Boolean = true
  }

  def swaggerConfig: Swagger = new Swagger().basePath(basePath).host(host).info(info).scheme(scheme)

  def reader: Reader = new Reader(swaggerConfig, readerConfig)
  def swagger: Swagger = reader.read(apiTypes.map(t ⇒ {
    Class.forName(getClassNameForType(t))
  }).toSet)

  def toJsonString(s: Swagger): String = {
    Json.mapper()
      .writeValueAsString(s)
  }

  lazy val routes: Route = get {
    import MediaTypes._

    pathPrefix(basePath) {
      path("swagger.json") {
        respondWithMediaType(`application/json`) {
          complete(toJsonString(swagger))
        }
      }
    }
  }

  def getClassNameForType(t: Type): String ={
    val typeSymbol = t.typeSymbol
    val fullName = typeSymbol.fullName
    if (typeSymbol.isModuleClass) {
      val idx = fullName.lastIndexOf('.')
      if (idx >=0) {
        val mangledName = s"${fullName.slice(0, idx)}$$${fullName.slice(idx+1,fullName.size)}$$"
        mangledName
      } else fullName
    } else fullName
  }
}
