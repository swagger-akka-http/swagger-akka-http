package com.github.swagger.akka.javadsl

import java.util

import com.github.swagger.akka.samples.DictHttpService
import org.scalatest.{Matchers, WordSpec}

class SwaggerGeneratorSpec extends WordSpec with Matchers {

  "Java DSL SwaggerGenerator" should {
    "properly wrap the scala implementation" in {
      val generator = new SwaggerGenerator {
        override def apiClasses: util.Set[Class[_]] = java.util.Collections.singleton(classOf[DictHttpService])
      }
      generator.generateSwaggerJson should not be empty
      generator.generateSwaggerYaml should not be empty
    }
  }
}
