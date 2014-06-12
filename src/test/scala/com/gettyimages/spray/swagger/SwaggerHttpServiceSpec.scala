package com.gettyimages.spray.swagger

import org.scalatest.WordSpec
import org.scalatest.matchers.ShouldMatchers
import spray.testkit._
import scala.reflect.runtime.universe._
import akka.actor.ActorSystem
import spray.http._
import org.json4s.jackson.JsonMethods._
import org.json4s._

class SwaggerHttpServiceSpec
  extends WordSpec
  with ShouldMatchers
  with ScalatestRouteTest {

   val swaggerService = new SwaggerHttpService {
      override def apiTypes = Seq(typeOf[PetHttpService], typeOf[UserHttpService])
      override def apiVersion = "2.0"
      override def baseUrl = "http://some.domain.com/api"
      override def docsPath = "docs-are-here"
      override def actorRefFactory = ActorSystem("swagger-spray-test")
      //apiInfo, not used
      //authorizations, not used
   }

  implicit val formats = org.json4s.DefaultFormats

  "The SwaggerHttpService" when {
    "accessing the root doc path" should {
      "return the basic set of api info" in {
        Get("/docs-are-here") ~> swaggerService.routes ~> check {
          handled shouldBe true
          contentType shouldBe ContentTypes.`application/json`
          val response = parse(responseAs[String])
          (response \ "apiVersion").extract[String] shouldEqual "2.0"
          (response \ "swaggerVersion").extract[String] shouldEqual "1.2"
          val apis = (response \ "apis").extract[Array[JValue]]
          apis.size shouldEqual 2
          (apis(0) \ "description").extract[String] shouldEqual "Operations about pets."
          (apis(0) \ "path").extract[String] shouldEqual "/pet"
          //need api info
        }
      }
    }
    "accessing a sub-resource" should {
      "return the api description" in {
        Get("/docs-are-here/pet") ~> swaggerService.routes ~> check {
          handled shouldBe true
          contentType shouldBe ContentTypes.`application/json`

          val response = parse(responseAs[String])
          (response \ "apiVersion").extract[String] shouldEqual "2.0"
          (response \ "resourcePath").extract[String] shouldEqual "/pet"
          val apis = (response \ "apis").extract[Array[JValue]]
          apis.size shouldEqual 2
          (apis(0) \ "path").extract[String] shouldEqual "/pet/{petId}"
          val ops = (apis(1) \ "operations").extract[Array[JValue]]
          ops.size shouldEqual 2
          val models = (response \ "models").extract[JObject]
          val pet = (models \ "Pet").extract[JObject]
          (pet \ "id").extract[String] shouldEqual "Pet"
          (pet \ "properties" \ "id" \ "type").extract[String] shouldEqual "integer"
          (pet \ "properties" \ "id" \ "format").extract[String] shouldEqual "int32"
          (pet \ "properties" \ "name" \ "type").extract[String] shouldEqual "string"
          (pet \ "properties" \ "birthDate" \ "type").extract[String] shouldEqual "string"
          (pet \ "properties" \ "birthDate" \ "format").extract[String] shouldEqual "date-time"

        }
      }
    }
  }

}
