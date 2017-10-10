package com.github.swagger.akka

import scala.collection.JavaConverters._
import org.json4s._
import org.json4s.native.JsonMethods._
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}
import org.yaml.snakeyaml.Yaml
import com.github.swagger.akka.model._
import com.github.swagger.akka.samples._
import akka.http.scaladsl.model.ContentTypes
import akka.http.scaladsl.testkit.ScalatestRouteTest

class MinimalSwaggerHttpServiceSpec
    extends WordSpec with Matchers with BeforeAndAfterAll with ScalatestRouteTest {

  override def afterAll: Unit = {
    super.afterAll()
    system.terminate()
  }

  val swaggerService = new SwaggerHttpService {
    override val apiClasses: Set[Class[_]] = Set(classOf[PetHttpService], classOf[UserHttpService])
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
          (response \ "host") shouldBe JNothing
          (response \ "schemes").extract[List[String]] shouldEqual List("http", "https")
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
      "return the basic set of api info in yaml" in {
        Get(s"/${swaggerService.apiDocsPath}/swagger.yaml") ~> swaggerService.routes ~> check {
          handled shouldBe true
          contentType shouldBe CustomMediaTypes.`text/vnd.yaml`.toContentType
          val str = responseAs[String]
          val yamlMap = new Yaml().load(str).asInstanceOf[java.util.Map[String, Object]].asScala
          yamlMap.getOrElse("swagger", "-1.0") shouldEqual "2.0"
          yamlMap.contains("host") shouldBe false
        }
      }
    }
  }

}
