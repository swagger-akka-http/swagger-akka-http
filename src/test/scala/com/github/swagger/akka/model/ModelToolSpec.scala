package com.github.swagger.akka.model

import scala.collection.JavaConverters._
import com.github.swagger.akka.SwaggerHttpService
import com.github.swagger.akka.samples.{PetHttpService, UserHttpService}
import io.swagger.models.{Model, Contact => SwaggerContact, Info => SwaggerInfo, License => SwaggerLicense}
import org.scalatest.{Matchers, WordSpec}

class ModelToolSpec extends WordSpec with Matchers {

  "swaggerToScala" should {
    "support roundtrip" in {
      val swaggerInfo = new SwaggerInfo().version("v1").description("desc1").termsOfService("TOS1").title("title1")
        .contact(new SwaggerContact().name("contact-name1").email("contact@example.com").url("http://example.com/about"))
        .license(new SwaggerLicense().name("example-license").url("http://example.com/license"))
      val info: Info = swagger2scala(swaggerInfo)
      scala2swagger(info) shouldEqual swaggerInfo
    }
  }

  "asScala" should {
    "handle null" in {
      asScala(null.asInstanceOf[java.util.Map[String, Model]]) shouldEqual Map.empty[String, Model]
      asScala(null.asInstanceOf[java.util.Map[String, String]]) shouldEqual Map.empty[String, String]
      asScala(null.asInstanceOf[java.util.Set[String]]) shouldEqual Set.empty[String]
      asScala(null.asInstanceOf[java.util.Set[Model]]) shouldEqual Set.empty[Model]
      asScala(null.asInstanceOf[java.util.List[String]]) shouldEqual List.empty[String]
      asScala(null.asInstanceOf[java.util.List[Model]]) shouldEqual List.empty[Model]
      asScala(null.asInstanceOf[java.util.Optional[String]]) shouldEqual None
      asScala(null.asInstanceOf[java.util.Optional[Model]]) shouldEqual None
    }
    "handle simple java map" in {
      val swaggerService = new SwaggerHttpService {
        override val apiClasses: Set[Class[_]] = Set(classOf[PetHttpService], classOf[UserHttpService])
      }
      val definitions = swaggerService.filteredSwagger.getDefinitions
      definitions should not be null
      definitions should have size 4
      val smap = asScala(definitions)
      smap should contain theSameElementsAs definitions.asScala
    }
  }
}