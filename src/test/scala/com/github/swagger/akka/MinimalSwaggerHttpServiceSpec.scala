package com.github.swagger.akka

import scala.reflect.runtime.universe._
import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.scalatest.Matchers
import org.scalatest.WordSpec
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
import akka.http.scaladsl.testkit._
import akka.http.scaladsl.unmarshalling._
import akka.stream.ActorMaterializer
import spray.json._
import spray.json.DefaultJsonProtocol._

class MinimalSwaggerHttpServiceSpec
    extends WordSpec
    with Matchers
    with ScalatestRouteTest {

  val myMaterializer = materializer

  val swaggerService = new SwaggerHttpService with HasActorSystem {
    override implicit val actorSystem: ActorSystem = system
    override implicit val materializer: ActorMaterializer = myMaterializer
    override val apiTypes = Seq(typeOf[PetHttpService], typeOf[UserHttpService])
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
          (response \ "schemes").extract[List[String]] shouldEqual List("http")
          (response \ "basePath").extract[String] shouldEqual s"${swaggerService.basePath}"
          (response \ "externalDocs").extract[Option[Map[String, String]]] shouldEqual None
          (response \ "securityDefinitions").extract[Map[String, String]] shouldEqual Map()
          val paths = (response \ "paths").extract[JObject]
          paths.values.size shouldEqual 2
          val userPath = (paths \ "/user")
          (userPath \ "get" \ "summary").extract[String] shouldEqual "Get user by name"
          (response \ "info" \ "description").extract[String] shouldEqual ""
          (response \ "info" \ "title").extract[String] shouldEqual ""
          (response \ "info" \ "termsOfService").extract[String] shouldEqual ""
          (response \ "info" \ "version").extract[String] shouldEqual ""
          (response \ "info" \ "contact").extract[Option[Contact]] shouldEqual None
          (response \ "info" \ "license").extract[Option[License]] shouldEqual None
        }
      }
    }
  }

}
