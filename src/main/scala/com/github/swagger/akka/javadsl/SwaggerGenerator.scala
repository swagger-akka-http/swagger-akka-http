package com.github.swagger.akka.javadsl

import java.util

import scala.collection.JavaConverters._
import scala.compat.java8.OptionConverters._
import io.swagger.models.{ExternalDocs, Info, Scheme}
import io.swagger.models.auth.SecuritySchemeDefinition

trait SwaggerGenerator {
  def apiClasses: util.Set[Class[_]]
  def host: String = ""
  def basePath: String = "/"
  def apiDocsPath: String = "api-docs"
  def info: Info = new Info()
  def scheme: Scheme = Scheme.HTTP
  def securitySchemeDefinitions: util.Map[String, SecuritySchemeDefinition] = util.Collections.emptyMap()
  def externalDocs: util.Optional[ExternalDocs] = util.Optional.empty()
  def vendorExtensions: util.Map[String, Object] = util.Collections.emptyMap()
  def unwantedDefinitions: util.List[String] = util.Collections.emptyList()

  private[javadsl] lazy val converter = new Converter(this)

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
  override def externalDocs: Option[ExternalDocs] = javaGenerator.externalDocs.asScala
  override def vendorExtensions: Map[String, Object] = javaGenerator.vendorExtensions.asScala.toMap
  override def unwantedDefinitions: Seq[String] = javaGenerator.unwantedDefinitions.asScala
}
