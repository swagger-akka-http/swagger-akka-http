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

class SwaggerApiBuilderSpec extends WordSpec with ShouldMatchers {
  /*
  val swaggerApi = new SwaggerApiBuilder("1.2", "1.0", "swagger-specs", _: Seq[Type], _: Seq[Type]),c

  "A SwaggerApiBuilder" when {
    "passed a test api" should {
      "throw an IllegalArgumentException if it has no annotation" in {
        intercept[IllegalArgumentException] {
          swaggerApi(List(typeOf[TestApiWithNoAnnotation]), List[Type]())
        }
        }
        "throw an IllegalArgumentException if it has the wrong annotation" in {
          intercept[IllegalArgumentException] {
            swaggerApi(List(typeOf[TestApiWithWrongAnnotation]), List[Type]())
      }
          }
          "throw an IllegalArgumentException if it doesn't extend HttpService" in {
            intercept[IllegalArgumentException] {
              swaggerApi(List(typeOf[TestApiDoesNotExtendHttpService]), List[Type]())
        }
            }
            "handle a properly annotated HttpService" in {
          }
            }
            "passed a test api with data only" should {
              "output that data model" in {
                val api = swaggerApi(List(typeOf[TestApiWithOnlyDataType]), List(typeOf[TestModel], typeOf[TestModelNode]))
                val (_, apiListings) = api.buildAll
                apiListings should contain key ("/test")
                val apiListing = apiListings("/test")
                apiListing.models should be ('defined)
                apiListing.models.get should contain key ("TestModel")
    }
              }
            }
            "passed a test api with a sub path with path parameters" should {
              "output api on that sub path and test parameters identified" in {
                val api = swaggerApi(List(typeOf[TestApiWithPathOperation]), List(typeOf[TestModel], typeOf[TestModelNode]))
                val (_, apiListings) = api.buildAll
                apiListings should contain key ("/test")
                val apiListing = apiListings("/test")
                val operations = apiListing.apis
                operations should have size (2)
                operations(0).path should be ("/test/sub/{someParam}/path/{anotherParam}")
                operations(1).path should be ("/test/other/sub/{someParam}/path/{anotherParam}")
                }
              }
              "passed a test api with a method returning complex entity" should {
                "respect model class hierarchy" in {
                  val api = swaggerApi(List(typeOf[TestApiWithParamsHierarchy]), List(typeOf[ModelBase], typeOf[ModelExtension]))
                  val (_, apiListings) = api.buildAll
                  apiListings should contain key ("/test")
                  val apiListing = apiListings("/test")
                  val operations = apiListing.apis
                  operations should have size (1)
                  operations(0).path should be ("/test/paramHierarchyOperation")
                  val model = apiListing.models.get("ModelExtension")
                  model.properties("date").`type` should be ("dateTime")
                  model.properties("name").`type` should be ("string")
            }
                }
                "passed a test api with explicit operation positions" should {
                  "output the operations in position order" in {
                    val api = swaggerApi(List(typeOf[TestApiWithOperationPositions]), List(typeOf[ModelBase], typeOf[ModelExtension]))
                    val (_, apiListings) = api.buildAll
                    apiListings should contain key ("/test")
                    val apiListing = apiListings("/test")
                    val operations = apiListing.apis
                    operations should have size 3
                    operations(0).operations.get.apply(0).summary should be ("order0")
                    operations(0).operations.get.apply(1).summary should be ("order1")
                    operations(1).operations.get.apply(0).summary should be ("order2")
                    operations(1).operations.get.apply(1).summary should be ("order3")
                    operations(2).operations.get.apply(0).summary should be ("order4")
              }
                  }
*/

    val apiTypes = Seq(typeOf[PetHttpService], typeOf[UserHttpService])
    val config = new SwaggerConfig("myVersion", SwaggerSpec.version, "http://example.com", "")


      "The SwaggerApiBuilder class" when {
        "instantiating" should {
          "list all API specs" in {
            val listingMap = new SwaggerApiBuilder(config, apiTypes, Seq()).listings

            listingMap.size shouldBe 2
            listingMap.get("/user") shouldBe 'defined
            listingMap.get("/pet") shouldBe 'defined

            val petListing = listingMap.get("/pet").get

            petListing.apis.length shouldEqual 2
            petListing.apis.head.description shouldBe None
            petListing.apis.head.path shouldBe "/pet/{petId}"
            petListing.apis.head.operations.length shouldEqual 3

            petListing.apiVersion shouldBe "myVersion"
            petListing.basePath shouldBe "http://example.com"
            petListing.consumes shouldBe List()
            petListing.description shouldBe Some("Operations about pets.")
            //petListing.models shouldBe 'defined
            petListing.swaggerVersion shouldBe "1.2"

            val userListing = listingMap.get("/user").get
            userListing.produces.size shouldBe 1
            userListing.produces(0) shouldBe "application/json"
            userListing.apis.length shouldBe 2
            }
          }
        }

      }

