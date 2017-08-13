package com.github.swagger.akka.javadsl

import scala.collection.JavaConverters._
import io.swagger.models.{ExternalDocs, Info, Scheme}
import io.swagger.models.auth.SecuritySchemeDefinition

trait SwaggerGenerator {
  def apiClasses: java.util.Set[Class[_]]
  def host: String = ""
  def basePath: String = "/"
  def apiDocsPath: String = "api-docs"
  def info: Info = new Info()
  def scheme: Scheme = Scheme.HTTP
  def securitySchemeDefinitions: java.util.Map[String, SecuritySchemeDefinition] = java.util.Collections.emptyMap()
  def externalDocs: Option[ExternalDocs] = None
  def vendorExtensions: java.util.Map[String, Object] = java.util.Collections.emptyMap()
  def unwantedDefinitions: java.util.List[String] = java.util.Collections.emptyList()

  private lazy val converter = new Converter(this)

  def generateSwaggerJson: String = converter.generateSwaggerJson
  def generateSwaggerYaml: String = converter.generateSwaggerYaml
}

private class Converter(javaGenerator: SwaggerGenerator) extends com.github.swagger.akka.SwaggerGenerator {
  import com.github.swagger.akka.model.swagger2scala
  override def apiClasses: Set[Class[_]] = javaGenerator.apiClasses.asScala.toSet
  override def host: String = javaGenerator.host
  override def basePath: String = javaGenerator.basePath
  override def apiDocsPath: String = javaGenerator.apiDocsPath
  override def info: com.github.swagger.akka.model.Info = javaGenerator.info
  override def scheme: Scheme = javaGenerator.scheme
  override def securitySchemeDefinitions: Map[String, SecuritySchemeDefinition] = javaGenerator.securitySchemeDefinitions.asScala.toMap
  override def externalDocs: Option[ExternalDocs] = javaGenerator.externalDocs
  override def vendorExtensions: Map[String, Object] = javaGenerator.vendorExtensions.asScala.toMap
  override def unwantedDefinitions: Seq[String] = javaGenerator.unwantedDefinitions.asScala
}
