package com.github.swagger.akka.javadsl

import java.util

import io.swagger.v3.oas.models.{Components, ExternalDocumentation}
import com.github.swagger.akka.model.asScala
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityScheme

trait SwaggerGenerator {
  def apiClasses: util.Set[Class[_]]
  def host: String = ""
  def basePath: String = "/"
  def apiDocsPath: String = "api-docs"
  def info: Info = new Info()
  //def schemes: util.List[Scheme] = List(Scheme.HTTP).asJava
  def securitySchemes: util.Map[String, SecurityScheme] = util.Collections.emptyMap()
  def externalDocs: util.Optional[ExternalDocumentation] = util.Optional.empty()
  def vendorExtensions: util.Map[String, Object] = util.Collections.emptyMap()
  def unwantedDefinitions: util.List[String] = util.Collections.emptyList()

  private[javadsl] lazy val converter = new Converter(this)

  def generateSwaggerJson: String = converter.generateSwaggerJson
  def generateSwaggerYaml: String = converter.generateSwaggerYaml
}

private class Converter(javaGenerator: SwaggerGenerator) extends com.github.swagger.akka.SwaggerGenerator {
  import com.github.swagger.akka.model.swagger2scala
  override def apiClasses: Set[Class[_]] = asScala(javaGenerator.apiClasses)
  override def host: String = javaGenerator.host
  override def basePath: String = javaGenerator.basePath
  override def apiDocsPath: String = javaGenerator.apiDocsPath
  override def info: com.github.swagger.akka.model.Info = javaGenerator.info
  //override def schemes: List[Scheme] = asScala(javaGenerator.schemes)
  override def securitySchemes: Map[String, SecurityScheme] = asScala(javaGenerator.securitySchemes)
  override def externalDocs: Option[ExternalDocumentation] = asScala(javaGenerator.externalDocs)
  override def vendorExtensions: Map[String, Object] = asScala(javaGenerator.vendorExtensions)
  override def unwantedDefinitions: Seq[String] = asScala(javaGenerator.unwantedDefinitions)
}
