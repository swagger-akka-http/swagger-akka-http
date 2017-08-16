package com.github.swagger.akka.javadsl

import java.util

import scala.collection.JavaConverters._

import com.github.swagger.akka.samples.DictHttpService
import io.swagger.models.auth.{BasicAuthDefinition, SecuritySchemeDefinition}
import io.swagger.models._
import org.scalatest.{Matchers, WordSpec}

class SwaggerGeneratorSpec extends WordSpec with Matchers {

  "Java DSL SwaggerGenerator" should {
    "not fail when generating swagger doc" in {
      val generator = new SwaggerGenerator {
        override def apiClasses: util.Set[Class[_]] = util.Collections.singleton(classOf[DictHttpService])
      }
      generator.generateSwaggerJson should not be empty
      generator.generateSwaggerYaml should not be empty
    }

    "properly convert the javadsl settings" in {
      val contact = new Contact().email("a@b.com").name("a").url("http://b.com")
      val license = new License().name("z").url("http://b.com/license")
      val testInfo = new Info().contact(contact).description("desc").license(license)
            .termsOfService("T&C").title("Title").version("0.1")
      val securitySchemeDefinition = new BasicAuthDefinition()
      val edocs = new ExternalDocs().description("edesc").url("http://b.com/docs")
      val generator = new SwaggerGenerator {
        override def apiClasses: util.Set[Class[_]] = util.Collections.singleton(classOf[DictHttpService])
        override def host: String = "host:12345"
        override def basePath: String = "/base"
        override def apiDocsPath: String = "docs"
        override def info: Info = testInfo
        override def schemes: util.List[Scheme] = List(Scheme.HTTPS).asJava
        override def securitySchemeDefinitions: util.Map[String, SecuritySchemeDefinition] = {
          val jmap = new util.HashMap[String, SecuritySchemeDefinition]()
          jmap.put("basic", securitySchemeDefinition)
          jmap
        }
        override def externalDocs: util.Optional[ExternalDocs] = util.Optional.of(edocs)
        override def vendorExtensions: util.Map[String, Object] = {
          val jmap = new util.HashMap[String, Object]()
          jmap.put("n1", "v1")
          jmap
        }
        override def unwantedDefinitions: util.List[String] = util.Collections.singletonList("unwanted")
      }
      generator.converter.apiClasses shouldEqual Set(classOf[DictHttpService])
      generator.converter.host shouldEqual generator.host
      generator.converter.basePath shouldEqual generator.basePath
      generator.converter.apiDocsPath shouldEqual generator.apiDocsPath
      import com.github.swagger.akka.model.scala2swagger
      scala2swagger(generator.converter.info) shouldEqual testInfo
      generator.converter.schemes.asJava shouldEqual generator.schemes
      generator.converter.securitySchemeDefinitions.asJava shouldEqual generator.securitySchemeDefinitions
      generator.converter.externalDocs.get shouldEqual generator.externalDocs.get()
      generator.converter.vendorExtensions.asJava shouldEqual generator.vendorExtensions
    }
  }
}
