package com.github.swagger.spray.model

import io.swagger.models.{
Info ⇒ SwaggerInfo,
Contact ⇒ SwaggerContact,
License ⇒ SwaggerLicense
}
import org.scalatest.{Matchers, WordSpec}

class ModelToolSpec
  extends WordSpec
  with Matchers {
  
  "The model package object" should {
    "swaggerToScala roundtrip should work" in {
      val swaggerInfo = new SwaggerInfo().version("v1").description("desc1").termsOfService("TOS1").title("title1")
        .contact(new SwaggerContact().name("contact-name1").email("contact@example.com").url("http://example.com/about"))
        .license(new SwaggerLicense().name("example-license").url("http://example.com/license"))
      val info: Info = swagger2scala(swaggerInfo)
      scala2swagger(info) shouldEqual swaggerInfo
    }
  }
}