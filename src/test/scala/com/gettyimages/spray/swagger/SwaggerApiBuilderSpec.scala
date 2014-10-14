/**
 * Copyright 2014 Getty Imges, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gettyimages.spray.swagger

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.WordSpec
import scala.reflect.runtime.universe._
import com.wordnik.swagger.config._
import com.wordnik.swagger.core._

class SwaggerApiBuilderSpec
  extends WordSpec
  with ShouldMatchers {

  val apiTypes = Seq(typeOf[PetHttpService], typeOf[UserHttpService])
  val config = new SwaggerConfig("myVersion", SwaggerSpec.version, "http://example.com", "")

    val swaggerApi = new SwaggerApiBuilder(config, _: Seq[Type])

    "The SwaggerApiBuilder class" when {
      "passed an annoted service" should {
        "list all API specs" in {
          val listingMap = swaggerApi(apiTypes).listings

          listingMap.size shouldBe 2
          listingMap.get("/user") shouldBe 'defined
          listingMap.get("/pet") shouldBe 'defined

          val petListing = listingMap.get("/pet").get

          petListing.apis.length shouldEqual 2
          petListing.apis.filter(_.path == "/pet/{petId}").head.description shouldBe None
          petListing.apis.filter(_.path == "/pet/{petId}").head.path shouldBe "/pet/{petId}"
          petListing.apis.filter(_.path == "/pet/{petId}").head.operations.length shouldEqual 3

          petListing.apiVersion shouldBe "myVersion"
          petListing.basePath shouldBe "http://example.com"
          petListing.consumes shouldBe List("application/json", "application/vnd.test.pet")
          petListing.produces shouldBe List("application/json", "application/vnd.test.pet")
          petListing.description shouldBe Some("Operations about pets.")
          //petListing.models shouldBe 'defined
          petListing.swaggerVersion shouldBe "1.2"
          petListing.models shouldBe 'defined
          petListing.models.get.size shouldBe 1
          petListing.models.get("Pet").name shouldBe "Pet"

          val userListing = listingMap.get("/user").get
          userListing.produces.size shouldBe 1
          userListing.produces(0) shouldBe "application/json"
          userListing.apis.length shouldBe 2
        }
      }
      "passed a test api with data only" should {
        "output that data model" in {
          val listingsMap = swaggerApi(List(typeOf[TestApiWithOnlyDataType])).listings
          listingsMap should contain key ("/test")
          val apiListing = listingsMap("/test")
          apiListing.apis(0).operations(0).parameters(0).dataType shouldEqual "TestModel"
          apiListing.apis(0).operations(0).nickname shouldEqual "testOperation"
        }
      }
      "passed a test api with a sub path with path parameters" should {
        "output api on that sub path and test parameters identified" in {
          val apiListings = swaggerApi(List(typeOf[TestApiWithPathOperation])).listings
          apiListings should contain key ("/test")
          val apiListing = apiListings("/test")
          val operations = apiListing.apis.sortBy(_.path.length)
          operations should have size (2)
          operations(0).path should be ("/test/sub/{someParam}/path/{anotherParam}")
          operations(1).path should be ("/test/other/sub/{someParam}/path/{anotherParam}")
          }
        }
      "passed a test api with a method returning complex entity" should {
        "respect model class hierarchy" in {
          val apiListings = swaggerApi(List(typeOf[TestApiWithParamsHierarchy])).listings
          apiListings should contain key ("/test")
          val apiListing = apiListings("/test")
          val operations = apiListing.apis
          operations should have size (1)
          operations(0).path should be ("/test/paramHierarchyOperation")
          val model = apiListing.models.get("ModelExtension")
          model.properties("date").`type` should be ("Date")
          model.properties("name").`type` should be ("string")
        }
      }

      "passed a test api with explicit operation positions" should {
        "output the operations in position order" in {
          val apiListings = swaggerApi(List(typeOf[TestApiWithOperationPositions])).listings
          apiListings should contain key ("/test")
          val apiListing = apiListings("/test")
          val apis = apiListing.apis
          apis should have size 3
          apis(0).operations(0).summary should be ("order0")
          apis(0).operations(1).summary should be ("order1")
          apis(1).operations(0).summary should be ("order2")
          apis(1).operations(1).summary should be ("order3")
          apis(2).operations(0).summary should be ("order4")
        }
      }

      "passed a test api with basePath annotation" should {
        "output operation path with specified basePath" in {
          val apiListings = swaggerApi(List(typeOf[TestApiWithBasePathAnnotation])).listings
          apiListings should contain key ("/test-override")
          val apiListing = apiListings("/test-override")
          val operations = apiListing.apis
          operations should have size (1)
          operations(0).path should be ("/test-override/{pathParam}")
        }
      }
    }
  }
