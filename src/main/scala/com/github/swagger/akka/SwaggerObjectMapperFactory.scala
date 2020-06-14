package com.github.swagger.akka

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.databind.{ObjectMapper, ObjectWriter}
import io.swagger.util.ObjectMapperFactory

private object SwaggerObjectMapperFactory extends ObjectMapperFactory {
  lazy val jsonMapper: ObjectMapper = ObjectMapperFactory.createJson
  lazy val yamlMapper: ObjectMapper = ObjectMapperFactory.createYaml

  def jsonPretty: ObjectWriter = jsonMapper.writer(new DefaultPrettyPrinter)
  def yamlPretty: ObjectWriter = yamlMapper.writer(new DefaultPrettyPrinter)
}
