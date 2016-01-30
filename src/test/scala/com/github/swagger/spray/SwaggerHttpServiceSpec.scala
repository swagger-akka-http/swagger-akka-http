package com.github.swagger.spray

import com.github.swagger.spray.model.{License, Contact, Info}
import com.github.swagger.spray.samples._
import akka.actor.ActorRefFactory
import io.swagger.jaxrs.config.ReaderConfig
import org.json4s._
import org.json4s.jackson.Serialization
import org.scalatest.{Matchers, WordSpec}
import spray.http._
import spray.httpx.Json4sJacksonSupport
import spray.testkit._
import scala.collection.JavaConversions._
import scala.reflect.runtime.universe._

class SwaggerHttpServiceSpec
  extends WordSpec
  with Matchers
  with ScalatestRouteTest
  with Json4sJacksonSupport {

  val swaggerService = new SwaggerHttpService {
    override val apiTypes = Seq(
      typeOf[PetHttpService],
      typeOf[UserHttpService]
    )

    override val host = "some.domain.com"
    override val basePath = "api-doc"

    override val info: Info = Info(
      description = "Pets love APIs",
      version = "1.0",
      title = "Test API Service",
      termsOfService = "Lenient",
      contact = Some(Contact("James T. Kirk", "http://startrek.com", "captain@kirk.com")),
      license = Some(License("Apache", "http://license.apache.com")))

    implicit def actorRefFactory: ActorRefFactory = system
  }

  implicit val formats = org.json4s.DefaultFormats

  implicit val json4sJacksonFormats: Formats = Serialization.formats(NoTypeHints)


  "The SwaggerHttpService" when {
    "defining a derived service" should {
      "set the basePath" in {
        swaggerService.basePath should equal ("api-doc")
      }
    }
    "accessing the root doc path" should {
      "return the basic set of api info" in {
        Get("http://some.domain.com/api-doc/swagger.json") ~> swaggerService.routes ~> check {
          handled shouldBe true
          status.intValue should be (200)
          contentType should be (ContentTypes.`application/json`)
          val resp: JValue = responseAs[JValue]
          (resp \ "swagger").extract[String] should equal ("2.0")
          val paths = (resp \ "paths")
          paths.children.size shouldEqual 4
          val petPath = (paths \ "/pet")
          (petPath \ "post" \ "summary").extract[String] shouldEqual "Add a new pet to the store"
          (resp \ "info" \ "version").extract[String] shouldEqual "1.0"
          (resp \ "info" \ "description").extract[String] shouldEqual "Pets love APIs"
          val definitions = (resp \ "definitions")

          // Includes Function1RequestContextBoxedUnit which is an error at some level
          definitions.children.size shouldEqual (4)
          val pet = (definitions \ "Pet")
          (pet \ "properties" \ "name" \ "type").extract[String] shouldEqual ("string")
          val user = (definitions \ "User")
          (user \ "properties" \ "username" \ "type").extract[String] shouldEqual ("string")
          val petIdPath = (paths \ "/pet/{petId}")
          val delPetParams = (petIdPath \ "delete" \ "parameters")
          delPetParams.children should have size (1)
          val petIdOpt = delPetParams.find(pp => {
            (pp \ "name").extract[String] == "petId"
          })
          petIdOpt should be ('defined)
          (petIdOpt.get \ "in").extract[String] shouldEqual ("path")

          // Check for the owner sub-resource
          val ownerPath = (paths \ "/pet/{petId}/owner")
          (ownerPath \ "get" \ "operationId").extract[String] should equal ("readOwner")
        }
      }
    }

    "defining an HttpService on an inner object" should {
      "return the basic set of api info" in {
        val svc = new NestedService(system)

        Get("http://some.domain.com/api-doc/swagger.json") ~> svc.swaggerService.routes ~> check {
          handled shouldBe true
          status.intValue should be (200)
          contentType should be (ContentTypes.`application/json`)
          val resp: JValue = responseAs[JValue]
          (resp \ "swagger").extract[String] should equal ("2.0")
          (resp \ "paths" \ "/dogs" \ "get" \ "operationId").extract[String] shouldEqual ("getDogs")
        }
      }
    }
  }
}
