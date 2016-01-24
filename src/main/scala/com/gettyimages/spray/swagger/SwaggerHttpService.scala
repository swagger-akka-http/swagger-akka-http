package com.gettyimages.spray.swagger

import org.json4s.JObject
import org.json4s.jackson.Serialization
import spray.http.MediaTypes
import spray.httpx.Json4sJacksonSupport
import scala.collection.JavaConversions._
import com.gettyimages.spray.swagger.model._

import io.swagger.jaxrs.Reader
import io.swagger.jaxrs.config.ReaderConfig
import io.swagger.models.{Scheme, Swagger}
import io.swagger.util.Json

import spray.json.{JsObject, pimpString}
import spray.routing.{ HttpService, Route }

import scala.reflect.runtime.universe.Type

/**
 * @author rleibman
 */

trait SwaggerHttpService extends HttpService with Json4sJacksonSupport {
  val apiTypes: Seq[Type]
  val host: String
  val basePath: String
  val description = ""
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

  override def json4sJacksonFormats = org.json4s.DefaultFormats

  def toJsObject(s: Swagger): JsObject ={
    val result = Json.mapper()
      .writeValueAsString(s)
      .parseJson
      .asJsObject
    result
  }

  def toJObject(s: Swagger): JObject ={
    implicit val fmts = org.json4s.DefaultFormats
    val jString = toJsObject(s).compactPrint
    val jObj = Serialization.read[JObject](jString)
    jObj
  }

  lazy val routes: Route = get {
    import MediaTypes._

    pathPrefix(basePath) {
      path("swagger.json") {
        respondWithMediaType(`application/json`) {
          complete(toJObject(swagger))
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
