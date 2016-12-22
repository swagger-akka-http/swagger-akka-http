package com.github.swagger.spray

import com.github.swagger.spray.model.{Contact, Info, License}
import com.github.swagger.spray.samples._
import akka.actor.ActorRefFactory
import io.swagger.models.ExternalDocs
import io.swagger.models.auth.BasicAuthDefinition
import org.json4s._
import org.json4s.jackson.Serialization
import org.scalatest.{Matchers, WordSpec}
import spray.http._
import spray.httpx.Json4sJacksonSupport
import spray.routing.HttpService
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

    override val host = "some.domain.com:12345"
    override val basePath = "api"
    override val apiDocsPath = "api-doc"

    override val info: Info = Info(
      description = "Pets love APIs",
      version = "1.0",
      title = "Test API Service",
      termsOfService = "Lenient",
      contact = Some(Contact("James T. Kirk", "http://startrek.com", "captain@kirk.com")),
      license = Some(License("Apache", "http://license.apache.com")))
      
    override val externalDocs = Some(new ExternalDocs("my docs", "http://com.example.com/about"))
    override val securitySchemeDefinitions = Map("basicAuth" -> new BasicAuthDefinition())

    implicit def actorRefFactory: ActorRefFactory = system
  }

  implicit val formats = org.json4s.DefaultFormats

  implicit val json4sJacksonFormats: Formats = Serialization.formats(NoTypeHints)


  "The SwaggerHttpService" when {
    "defining a derived service" should {
      "set the basePath" in {
        swaggerService.basePath should equal ("api")
      }
      "set the apiDocsPath" in {
        swaggerService.apiDocsPath should equal ("api-doc")
      }
      "prependSlashIfNecessary adds a slash" in {
        swaggerService.prependSlashIfNecessary("/api-doc") should equal ("/api-doc")
      }
      "prependSlashIfNecessary does not need to add a slash" in {
        swaggerService.prependSlashIfNecessary("/api-doc") should equal ("/api-doc")
      }
      "removeInitialSlashIfNecessary removes a slash" in {
        swaggerService.removeInitialSlashIfNecessary("/api-doc") should equal ("api-doc")
      }
      "removeInitialSlashIfNecessary does not need to remove a slash" in {
        swaggerService.removeInitialSlashIfNecessary("api-doc") should equal ("api-doc")
      }
      def handle(tp: String, handle: Boolean, split: Boolean = true): Unit = {
        val routes = new HttpService {
          implicit def actorRefFactory: ActorRefFactory = system
          lazy val routes =
            if (split) path(swaggerService.splitOnSlash(tp) / "swagger.json") { get { complete("OK") } }
            else path(tp / "swagger.json") { get { complete("OK") } }
        }.routes
        Get(s"/$tp/swagger.json") ~> routes ~> check {
          if (!handle) handled should equal (false)
          else response.entity.asString should equal("\"OK\"")
        }
      }
      "splitOnSlash works without a slash" in {
        handle("api-doc", true)
        handle("/api-doc/post", false)
        handle("/api-doc-post", false)
        handle("/pre/api-doc", false)
        handle("/pre-api-doc", false)
      }
      "splitOnSlash works with a slash (and we don't match the path without it)" in {
        handle("v1/api-doc", true)
        handle("v1/api-doc", false, false)
      }
    }
    "accessing the root doc path" should {
      "return the basic set of api info" in {
        Get("http://some.domain.com/api-doc/swagger.json") ~> swaggerService.routes ~> check {
          handled shouldBe true
          status.intValue shouldBe 200
          contentType shouldBe ContentTypes.`application/json`
          val resp: JValue = responseAs[JValue]
          (resp \ "swagger").extract[String] shouldEqual "2.0"
          (resp \ "host").extract[String] shouldEqual swaggerService.host
          (resp \ "basePath").extract[String] shouldEqual s"/${swaggerService.basePath}"
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
          (pet \ "properties" \ "name" \ "type").extract[String] shouldEqual "string"
          val user = (definitions \ "User")
          (user \ "properties" \ "username" \ "type").extract[String] shouldEqual "string"
          val petIdPath = (paths \ "/pet/{petId}")
          val delPetParams = (petIdPath \ "delete" \ "parameters")
          delPetParams.children should have size (1)
          val petIdOpt = delPetParams.find(pp => {
            (pp \ "name").extract[String] == "petId"
          })
          petIdOpt should be ('defined)
          (petIdOpt.get \ "in").extract[String] shouldEqual "path"

          // Check for the owner sub-resource
          val ownerPath = (paths \ "/pet/{petId}/owner")
          (ownerPath \ "get" \ "operationId").extract[String] shouldEqual "readOwner"
          
          val ed = swaggerService.externalDocs.getOrElse(throw new IllegalArgumentException("missing external docs"))
          (resp \ "externalDocs").extract[Map[String, String]] shouldEqual Map(
              "description" -> ed.getDescription, "url" -> ed.getUrl)
          (resp \ "securityDefinitions" \ "basicAuth").extract[Map[String, String]] shouldEqual Map("type" -> "basic")
          
          (resp \ "info" \ "description").extract[String] shouldEqual swaggerService.info.description
          (resp \ "info" \ "title").extract[String] shouldEqual swaggerService.info.title
          (resp \ "info" \ "termsOfService").extract[String] shouldEqual swaggerService.info.termsOfService
          (resp \ "info" \ "version").extract[String] shouldEqual swaggerService.info.version
          (resp \ "info" \ "contact").extract[Option[Contact]] shouldEqual swaggerService.info.contact
          (resp \ "info" \ "license").extract[Option[License]] shouldEqual swaggerService.info.license
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
