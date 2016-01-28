package com.github.swagger.spray

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.reflect.runtime.universe._
import com.github.swagger.spray.model._
import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import io.swagger.jaxrs.Reader
import io.swagger.jaxrs.config.ReaderConfig
import io.swagger.models.Swagger
import io.swagger.util.Json
import spray.json.pimpString
import io.swagger.models.Scheme

/**
 * @author rleibman
 */
trait HasActorSystem {
  implicit val actorSystem: ActorSystem
  implicit val materializer: ActorMaterializer
}

trait SwaggerHttpService extends Directives with SprayJsonSupport {
  this: HasActorSystem ⇒

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

  def swaggerConfig = new Swagger().basePath(basePath).host(host).info(info).scheme(scheme)

  def reader = new Reader(swaggerConfig, readerConfig)
  def swagger: Swagger = reader.read(apiTypes.map(t ⇒ {
    Class.forName(getClassNameForType(t))
  }).toSet)
  
  val routes: Route = get {
    path("swagger.json") {
      complete {
        val result = Json.mapper().writeValueAsString(swagger).parseJson.asJsObject
        result
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