package com.gettyimages.spray.swagger

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.WordSpec
import spray.routing.HttpService
import akka.actor.Actor
import scala.reflect.runtime.universe._
import com.wordnik.swagger.annotations.Api
import com.wordnik.swagger.annotations.ApiOperation
import com.wordnik.swagger.annotations.ApiParamsImplicit
import com.wordnik.swagger.annotations.ApiParamImplicit

class SwaggerApiBuilderSpec extends WordSpec with ShouldMatchers {
  
  val swaggerApi = new SwaggerApiBuilder("1.2", "1.0", "swagger-specs", _: Seq[Type], _: Seq[Type])
  
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
}

abstract class TestApiWithNoAnnotation extends HttpService

@Deprecated
abstract class TestApiWithWrongAnnotation extends HttpService

@Api(value = "/test")
abstract class TestApiDoesNotExtendHttpService

@Api(value = "/test")
abstract class TestApiWithOnlyDataType extends HttpService {
  @ApiOperation(value = "testApiOperation", httpMethod = "GET")
  @ApiParamsImplicit(Array(new ApiParamImplicit(name = "test", value = "test param", dataType = "TestModel", paramType = "query")))
  def testOperation 
}
