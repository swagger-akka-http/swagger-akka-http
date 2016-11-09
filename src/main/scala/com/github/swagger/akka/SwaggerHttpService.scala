package com.github.swagger.akka

import scala.collection.JavaConverters._
import scala.reflect.runtime.universe.Type

import com.github.swagger.akka.model.Info
import com.github.swagger.akka.model.scala2swagger

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.ToResponseMarshallable.apply
import akka.http.scaladsl.server.Directive.addByNameNullaryApply
import akka.http.scaladsl.server.{Directives, Route}
import akka.stream.ActorMaterializer
import io.swagger.jaxrs.Reader
import io.swagger.jaxrs.config.ReaderConfig
import io.swagger.models.{ExternalDocs, Scheme, Swagger}
import io.swagger.models.auth.SecuritySchemeDefinition
import io.swagger.util.Json
import spray.json.pimpString

/**
 * @author rleibman
 */
trait HasActorSystem {
  implicit val actorSystem: ActorSystem
  implicit val materializer: ActorMaterializer
}

object SwaggerHttpService {
  val readerConfig = new ReaderConfig {
    def getIgnoredRoutes(): java.util.Collection[String] = List().asJavaCollection
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

  def reader = new Reader(swaggerConfig, readerConfig)
  def swagger: Swagger = reader.read(toJavaTypeSet(apiTypes).asJava)
  def prependSlashIfNecessary(path: String): String  = if(path.startsWith("/")) path else s"/$path" 
  def removeInitialSlashIfNecessary(path: String): String =
    if(path.startsWith("/")) removeInitialSlashIfNecessary(path.substring(1)) else path 
  
  def toJsonString(s: Swagger): String = Json.mapper().writeValueAsString(s)
  
  lazy val routes: Route =
    path(removeInitialSlashIfNecessary(apiDocsPath) / "swagger.json") {
      get {
        complete(toJsonString(swagger).parseJson.asJsObject)
      }
    }

}