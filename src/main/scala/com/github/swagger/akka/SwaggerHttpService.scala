package com.github.swagger.akka

import scala.collection.JavaConversions._
import scala.reflect.runtime.universe._

import com.github.swagger.akka.model._
import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.MediaTypes
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import io.swagger.jaxrs.Reader
import io.swagger.jaxrs.config.ReaderConfig
import io.swagger.models.{Scheme, Swagger}
import io.swagger.util.Json
import spray.json._

/**
 * @author rleibman
 */
trait HasActorSystem {
  implicit val actorSystem: ActorSystem
  implicit val materializer: ActorMaterializer
}

object SwaggerHttpService {
  val readerConfig = new ReaderConfig {
    def getIgnoredRoutes(): java.util.Collection[String] = List()
    def isScanAllResources(): Boolean = false
  }
  
  def toJavaTypeSet(apiTypes: Seq[Type]): Set[Class[_]] ={
    apiTypes.map(t => Class.forName(getClassNameForType(t))).toSet
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

trait SwaggerHttpService extends Directives with SprayJsonSupport {
  this: HasActorSystem ⇒

  import SwaggerHttpService._
  val apiTypes: Seq[Type]
  val host: String = "localhost"
  val basePath: String = "/"
  val apiDocsPath: String = "api-docs"
  val info: Info = Info()
  val scheme: Scheme = Scheme.HTTP

  def swaggerConfig = new Swagger().basePath(prependSlashIfNecessary(basePath)).host(host).info(info).scheme(scheme)

  def reader = new Reader(swaggerConfig, readerConfig)
  def swagger: Swagger = reader.read(toJavaTypeSet(apiTypes))
  def prependSlashIfNecessary(path: String): String  = if(path.startsWith("/")) path else s"/$path" 
  def removeInitialSlashIfNecessary(path: String): String =
    if(path.startsWith("/")) removeInitialSlashIfNecessary(path.substring(1)) else path 
  
  def toJsonString(s: Swagger): String = Json.mapper().writeValueAsString(s)
  
  val routes: Route = get {
    path(removeInitialSlashIfNecessary(apiDocsPath) / "swagger.json") {
      complete(toJsonString(swagger).parseJson.asJsObject)
    }
  }

}