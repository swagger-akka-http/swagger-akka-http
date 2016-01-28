/**
 * Copyright 2014 Getty Images, Inc.
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
package com.github.swagger.spray

import io.swagger.jaxrs.Reader
import io.swagger.jaxrs.config.ReaderConfig
import io.swagger.models.parameters.BodyParameter
import io.swagger.models.properties.{RefProperty, StringProperty}
import io.swagger.models._
import io.swagger.util.Json
import io.swagger.config._
import org.scalatest.{Matchers, WordSpec}
import scala.reflect.runtime.universe._
import scala.collection.JavaConversions._
import com.github.swagger.spray.samples._

class SprayApiReaderSpec
    extends WordSpec
    with Matchers {

  val SWAGGER_VERSION = "2.0"
  val API_VERSION = "1.0"
  val BASE_PATH = "foo"
  val HOST = "www.example.com"

  val swaggerInfo = new Info().version(API_VERSION)
  val readerConfig = new ReaderConfig {
    def getIgnoredRoutes(): java.util.Collection[String] = List()
    def isScanAllResources(): Boolean = false
  }

  def toJavaTypeSet(apiTypes: Seq[Type]): Set[Class[_]] ={
    apiTypes.map(t => Class.forName(t.toString())).toSet
  }

  "The Reader object" when {
    "passed an api with no annotation" should {
      "product a Swagger instance without any paths" in {
        val swaggerConfig = new Swagger().basePath(BASE_PATH).info(swaggerInfo)
        val reader = new Reader(swaggerConfig, readerConfig)
        val swagger = reader.read(toJavaTypeSet(Seq(typeOf[TestApiWithNoAnnotation])))
        swagger.getPaths() should be(null)
      }
    }

    "passed a properly annotated HttpService" should {
      val swaggerConfig = new Swagger()
        .host(HOST)
        .scheme(Scheme.HTTP)
        .basePath(BASE_PATH).info(swaggerInfo)
      val reader = new Reader(swaggerConfig, readerConfig)
      val swagger: Swagger = reader.read(toJavaTypeSet(Seq(typeOf[DictHttpService])))

      val info = model.swagger2scala(swagger.getInfo())

      "set the swagger version" in {
        swagger.getSwagger() shouldBe SWAGGER_VERSION
      }

      "set the API version" in {
        info.version shouldBe API_VERSION
      }

      "set the basePath as the relative path from the config" in {
        swagger.getBasePath() shouldBe BASE_PATH
      }

      "set the host as the host from the config" in {
        swagger.getHost() shouldBe HOST
      }

      "return an ApiListing" in {
        swagger.getPaths() should have size (1)
      }

      val dictPath = swagger.getPaths().get("/dict")

      "set the resourcePath as the value of the annotation" in {
        dictPath should not be (null)
      }

      val dictOperations = dictPath.getOperations()

      "set the operations based on the annotations" in {
        dictOperations should have size (2)
      }

      val getOperation = dictPath.getGet()

      "define the GET operation" in {
        getOperation should not be null
      }

      "set the GET operationId from the nickname" in {
        getOperation.getOperationId() shouldEqual ("someothername")
      }

      "set the GET operation description from the notes" in {
        getOperation.getDescription() shouldEqual ("Will look up the dictionary entry for the provided key.")
      }

      "set the GET operation summary from the value" in {
        getOperation.getSummary() shouldEqual ("Find entry by key.")
      }

      val getParams = getOperation.getParameters()

      "define the GET operation parameters" in {
        getParams should have size (1)
      }

      val getParam = getParams.get(0)

      "require the GET operation path parameter" in {
        getParam.getRequired() should be(true)
      }

      "should set the GET operation description from the api annotation" in {
        getParam.getDescription() shouldEqual "Keyword for the dictionary entry."
      }

      "define the GET 404 response message" in {
        val resp404 = getOperation.getResponses().get("404")
        resp404 should not be (null)
        resp404.getDescription() should equal("Dictionary does not exist.")
      }

      val postOperation = dictPath.getPost()

      "define the POST operation" in {
        postOperation should not be null
      }

      "default the POST operationId to the method name" in {
        postOperation.getOperationId() shouldEqual ("createRoute")
      }

      "set the POST operation description from the notes" in {
        postOperation.getDescription() shouldEqual ("Will add new entry to the dictionary, indexed by key, with an optional expiration value.")
      }

      "set the POST operation summary from the value" in {
        postOperation.getSummary() shouldEqual ("Add dictionary entry.")
      }

      val postParams = postOperation.getParameters()

      "set the POST operation parameters" in {
        postParams should have size (1)
      }

      val postParam = postParams.get(0)

      "should require the POST operation body parameter" in {
        postParam.getRequired() should be(true)
      }

      "set the POST description from the api annotation" in {
        postParam.getDescription() shouldEqual "Key/Value pair of dictionary entry, with optional expiration time."
      }

      "define the POST 400 response message" in {
        val resp400 = postOperation.getResponses().get("400")
        resp400 should not be (null)
        resp400.getDescription() should equal("Client Error")
      }

      "define the Model from the ApiImplicitParam dataType" in {
         val defMap = swagger.getDefinitions()
         defMap should not be (null)
         val dictEntryDef = defMap.get("DictEntry")
         dictEntryDef should not be (null)
         dictEntryDef.getProperties() should have size (3)
      }
    }

    "passed a service referencing a dataType" should {
      val swaggerConfig = new Swagger().basePath(BASE_PATH).info(swaggerInfo)
      val reader = new Reader(swaggerConfig, readerConfig)
      val swagger: Swagger = reader.read(toJavaTypeSet(Seq(typeOf[TestApiWithOnlyDataType])))
      val defMap = swagger.getDefinitions()
      "contain the dataType in the definitions" in {
        defMap should not be (null)
        defMap should have size (2)
        val testModelDef = defMap.get("TestModel")
        testModelDef should not be (null)
        testModelDef.getProperties() should have size (11)
      }
      "define the range of enum values" in {
        val testEnumProp = defMap.get("TestModel").
          getProperties().
          get("testEnum").
          asInstanceOf[StringProperty]
        testEnumProp should not be (null)
        val enumVals = testEnumProp.getEnum()
        enumVals should not be (null)
        enumVals should contain ("a")
        enumVals should contain ("b")
      }
    }

    "passed a service that does not extend an HttpService" should {
      val swaggerConfig = new Swagger().basePath(BASE_PATH).info(swaggerInfo)
      val reader = new Reader(swaggerConfig, readerConfig)
      val swagger: Swagger = reader.read(toJavaTypeSet(Seq(typeOf[TestApiDoesNotExtendHttpService])))
      "build the Swagger definition anyway" in {
        swagger.getPaths() should have size (1)

        swagger.getPaths().get("/test") should not be (null)

        val getOperation = swagger.getPaths().get("/test").getGet()
        getOperation should not be (null)
      }
    }

    "passed a service that uses sub-paths" should {
      val swaggerConfig = new Swagger().basePath(BASE_PATH).info(swaggerInfo)
      val reader = new Reader(swaggerConfig, readerConfig)
      val swagger: Swagger = reader.read(toJavaTypeSet(Seq(typeOf[TestApiWithPathOperation])))

      "define the sub-path operations" in {
        swagger.getPaths() should have size (2)
      }

      "define the sub-path operations based on the Path annotation" in {
        val path1 = swagger.getPaths().get("/test/sub/{someParam}/path/{anotherParam}")
        path1 should not be (null)
        path1.getGet().getOperationId() should equal ("subPathOperation")
        val path2 = swagger.getPaths().get("/test/other/sub/{someParam}/path/{anotherParam}")
        path2 should not be (null)
        path2.getGet().getOperationId() should equal ("otherSubPathOperation")
      }
    }

    "passed a service with a parameter hierarchy" should {
      val swaggerConfig = new Swagger().basePath(BASE_PATH).info(swaggerInfo)
      val reader = new Reader(swaggerConfig, readerConfig)
      val swagger: Swagger = reader.read(toJavaTypeSet(Seq(typeOf[TestApiWithParamsHierarchy])))

      "define the path hierarchy" in {
        swagger.getPaths() should have size (1)
        val path1 = swagger.getPaths().get("/test/paramHierarchyOperation")
        path1 should not be (null)
        path1.getGet().getOperationId() should equal ("paramHierarchyOperation")
      }

      "define the model from the response" in {
        val modelDef = swagger.getDefinitions().get("ModelExtension")
        modelDef should not be (null)
      }
    }

    "passed a service with operations defined by position" ignore {
      val swaggerConfig = new Swagger().basePath(BASE_PATH).info(swaggerInfo)
      val reader = new Reader(swaggerConfig, readerConfig)
      val swagger: Swagger = reader.read(toJavaTypeSet(Seq(typeOf[TestApiWithOperationPositions])))

      "sequence the operations by position" in {
        swagger.getPaths() should have size (3)
        val path0 = swagger.getPaths().get("/test/path0")
        path0 should not be (null)
        val pOps = path0.getOperations()
        pOps should have size (2)
        pOps.head.getSummary() should equal ("order0")
        pOps.tail.head.getSummary() should equal ("order1")
        val path1 = swagger.getPaths().get("/test/path1")
        path1 should not be (null)
        val pOps1 = path1.getOperations()
        pOps1 should have size (2)
        pOps1.head.getSummary() should equal ("order2")
        pOps1.tail.head.getSummary() should equal ("order3")

        val path2 = swagger.getPaths().get("/test/path2")
        path2 should not be (null)
        val pOps2 = path2.getOperations()
        pOps2 should have size (1)
        pOps2.head.getSummary() should equal ("order4")
      }
    }

    "passed a service using a responseContainer" should {
      val swaggerConfig = new Swagger().basePath(BASE_PATH).info(swaggerInfo)
      val reader = new Reader(swaggerConfig, readerConfig)
      val swagger: Swagger = reader.read(toJavaTypeSet(Seq(typeOf[TestApiWithResponseContainer])))

      "define the operation" in {
        swagger.getPaths() should have size (1)
        val ops = swagger.getPaths().get("/test").getOperations()
        ops should have size (1)
        val resp200 = ops.head.getResponses().get("200")
        resp200 should not be (null)
        resp200.getSchema().isInstanceOf[RefProperty] should be (true)
        val refProp = resp200.getSchema().asInstanceOf[RefProperty]
        refProp.getSimpleRef() should equal ("ListReply")
        // TODO - Needs to encode the ListReply[T] T-Val
      }
    }

    "passed a service with a dateTime parameter" should {
      val swaggerConfig = new Swagger().basePath(BASE_PATH).info(swaggerInfo)
      val reader = new Reader(swaggerConfig, readerConfig)
      val swagger: Swagger = reader.read(toJavaTypeSet(Seq(typeOf[TestApiWithDateTime])))

      "define the string data type with format date-time" in {
        swagger.getPaths() should have size (1)
        val ops = swagger.getPaths().get("/test").getOperations()
        ops should have size (1)
        val params = ops.head.getParameters
        params should have size (1)
        val param = params.head
        val modelOpt = param match {
          case bp: BodyParameter => Some(bp.getSchema)
          case _=> None
        }
        modelOpt should be ('defined)
        val miOpt = modelOpt.get match {
          case mi: ModelImpl => Some(mi)
          case _ => None
        }
        miOpt should be ('defined)
        miOpt.get.getType should equal ("string")
        miOpt.get.getFormat should equal ("date-time")
      }
    }

    "passed a service with an ApiResponse" should {
      val swaggerConfig = new Swagger().basePath(BASE_PATH).info(swaggerInfo)
      val reader = new Reader(swaggerConfig, readerConfig)
      val swagger: Swagger = reader.read(toJavaTypeSet(Seq(typeOf[TestApiWithApiResponse])))

      "define the operation" in {
        swagger.getPaths() should have size (1)
        val ops = swagger.getPaths().get("/test").getOperations()
        ops should have size (1)
        val resp200 = ops.head.getResponses().get("200")
        resp200 should not be (null)
      }
    }
  }
}
