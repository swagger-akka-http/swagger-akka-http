package com.github.swagger.akka.javadsl

import com.github.swagger.akka.model.asScala
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.{SecurityRequirement, SecurityScheme}
import io.swagger.v3.oas.models.{Components, ExternalDocumentation, SpecVersion}

import java.util

trait SwaggerGenerator {
  def apiClasses: util.Set[Class[_]]
  def apiDocsPath: String = "api-docs"

  /**
   * @return the host (and possibly, port) for the API (eg `api.example.com` or `localhost:8080`)
   *         - used in conjunction with the [[schemes]] - this is ignored if you have non-empty [[serverURLs]]
   */
  def host: String = ""

  /**
   * @return list of URL schemes (default is `["http"]`) - this is ignored if you have non-empty [[serverURLs]]
   */
  def schemes: util.List[String] = util.Collections.singletonList("http")

  /**
   * @return list of URLs (as strings) - if this list is empty, then the values are derived using the [[host]] and [[schemes]]
   */
  def serverURLs: util.List[String] = util.Collections.emptyList()

  def basePath: String = ""
  def info: Info = new Info()
  def components: util.Optional[Components] = util.Optional.empty()
  def security: util.List[SecurityRequirement] = util.Collections.emptyList()
  def securitySchemes: util.Map[String, SecurityScheme] = util.Collections.emptyMap()
  def externalDocs: util.Optional[ExternalDocumentation] = util.Optional.empty()
  def vendorExtensions: util.Map[String, Object] = util.Collections.emptyMap()
  def unwantedDefinitions: util.List[String] = util.Collections.emptyList()
  def specVersion: SpecVersion = SpecVersion.V30

  private[javadsl] lazy val converter = new Converter(this)

  def generateSwaggerJson: String = converter.generateSwaggerJson
  def generateSwaggerYaml: String = converter.generateSwaggerYaml
}

private class Converter(javaGenerator: SwaggerGenerator) extends com.github.swagger.akka.SwaggerGenerator {
  import com.github.swagger.akka.model.swagger2scala
  override def apiClasses: Set[Class[_]] = asScala(javaGenerator.apiClasses)
  override def host: String = javaGenerator.host
  override def schemes: List[String] = asScala(javaGenerator.schemes)
  override def serverURLs: Seq[String] = asScala(javaGenerator.serverURLs)
  override def basePath: String = javaGenerator.basePath
  override def apiDocsPath: String = javaGenerator.apiDocsPath
  override def info: com.github.swagger.akka.model.Info = javaGenerator.info
  override def components: Option[Components] = asScala(javaGenerator.components)
  override def security: List[SecurityRequirement] = asScala(javaGenerator.security)
  override def securitySchemes: Map[String, SecurityScheme] = asScala(javaGenerator.securitySchemes)
  override def externalDocs: Option[ExternalDocumentation] = asScala(javaGenerator.externalDocs)
  override def vendorExtensions: Map[String, Object] = asScala(javaGenerator.vendorExtensions)
  override def unwantedDefinitions: Seq[String] = asScala(javaGenerator.unwantedDefinitions)
  override def specVersion: SpecVersion = javaGenerator.specVersion
}
