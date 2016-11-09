package com.github.swagger.akka

import scala.reflect.runtime.universe._
import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.scalatest.{Matchers, WordSpec}
import com.github.swagger.akka.model._
import com.github.swagger.akka.samples._
import akka.actor.ActorSystem
import akka.http._
import akka.http.scaladsl._
import akka.http.scaladsl.client._
import akka.http.scaladsl.client.RequestBuilding._
import akka.http.scaladsl.marshalling._
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit._
import akka.http.scaladsl.unmarshalling._
import akka.stream.ActorMaterializer
import io.swagger.models.{ExternalDocs, Scheme}
import io.swagger.models.auth.BasicAuthDefinition
import spray.json._
import spray.json.DefaultJsonProtocol._

class SwaggerHttpServiceSpec
    extends WordSpec
    with Matchers
    with ScalatestRouteTest {

  val myMaterializer = materializer

  val swaggerService = new SwaggerHttpService with HasActorSystem {
    override implicit val actorSystem: ActorSystem = system
    override implicit val materializer: ActorMaterializer = myMaterializer
    override val apiTypes = Seq(typeOf[PetHttpService], typeOf[UserHttpService])
    override val basePath = "api"
    override val apiDocsPath = "api-doc"
    override val scheme = Scheme.HTTPS
    override val host = "some.domain.com:12345"
    override val info = Info(description = "desc1",
                   version = "v1.0",
                   title = "title1",
                   termsOfService = "Free and Open",
                   contact = Some(Contact(name = "Alice Smith", url = "http://com.example.com/alicesmith", email = "alice.smith@example.com")),
                   license = Some(License(name = "MIT", url = "https://opensource.org/licenses/MIT")))
    override val externalDocs = Some(new ExternalDocs("my docs", "http://com.example.com/about"))
    override val securitySchemeDefinitions = Map("basicAuth" -> new BasicAuthDefinition())
  }

  implicit val formats = org.json4s.DefaultFormats

  "The SwaggerHttpService" when {
    "accessing the root doc path" should {
      "return the basic set of api info" in {
        Get(s"/${swaggerService.apiDocsPath}/swagger.json") ~> swaggerService.routes ~> check {
          handled shouldBe true
          contentType shouldBe ContentTypes.`application/json`
          val str = responseAs[String]
          val response = parse(str)
          (response \ "swagger").extract[String] shouldEqual "2.0"
          (response \ "host").extract[String] shouldEqual swaggerService.host
          (response \ "schemes").extract[List[String]] shouldEqual List("https")
          (response \ "basePath").extract[String] shouldEqual s"/${swaggerService.basePath}"
          val ed = swaggerService.externalDocs.getOrElse(throw new IllegalArgumentException("missing external docs"))
          (response \ "externalDocs").extract[Map[String, String]] shouldEqual Map(
              "description" -> ed.getDescription, "url" -> ed.getUrl)
          (response \ "securityDefinitions" \ "basicAuth").extract[Map[String, String]] shouldEqual Map("type" -> "basic")
          val paths = (response \ "paths").extract[JObject]
          paths.values.size shouldEqual 2
          val userPath = (paths \ "/user")
          (userPath \ "get" \ "summary").extract[String] shouldEqual "Get user by name"
          (response \ "info" \ "description").extract[String] shouldEqual swaggerService.info.description
          (response \ "info" \ "title").extract[String] shouldEqual swaggerService.info.title
          (response \ "info" \ "termsOfService").extract[String] shouldEqual swaggerService.info.termsOfService
          (response \ "info" \ "version").extract[String] shouldEqual swaggerService.info.version
          (response \ "info" \ "contact").extract[Option[Contact]] shouldEqual swaggerService.info.contact
          (response \ "info" \ "license").extract[Option[License]] shouldEqual swaggerService.info.license
        }
      }
    }

    "concatenated with other route" should {
      "not affect matching" in {
        val myRoute = path("p1" / "p2") {
          delete {
            complete("ok")
          }
        }
        Delete("/p1/wrong") ~> Route.seal(myRoute ~ swaggerService.routes) ~> check {
          status shouldNot be(StatusCodes.MethodNotAllowed)
          status shouldBe StatusCodes.NotFound
        }
      }
    }
  }

}
