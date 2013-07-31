package com.gettyimages.spray.swagger

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.WordSpec
import spray.routing.HttpService
import akka.actor.Actor
import scala.reflect.runtime.universe._
import com.wordnik.swagger.annotations.Api

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
  }

}

abstract class TestApiWithNoAnnotation extends HttpService

@Deprecated
abstract class TestApiWithWrongAnnotation extends HttpService

@Api(value = "/test")
abstract class TestApiDoesNotExtendHttpService