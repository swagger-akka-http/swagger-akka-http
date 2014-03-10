package com.gettyimages.spray.swagger

import org.scalatest.FunSpec
import akka.actor.{ActorRefFactory, ActorSystem}
import org.scalatest.matchers.ShouldMatchers
import scala.reflect.runtime.universe._
import spray.routing.Route

/**
 * Created by l-chan on 3/10/14.
 */
class SwaggerHttpServiceSpec extends FunSpec with ShouldMatchers {

  val context = ActorSystem("test")

  describe("initialization") {
    it("should not throw exceptions when implemented with defs") {
      new SwaggerHttpService {
        def actorRefFactory = context
        def apiTypes = Seq(typeOf[DictHttpService])
        def modelTypes = Seq()
        def apiVersion = "1.0"
        def swaggerVersion = "1.2"
        def baseUrl = ""
        def specPath = ""
        def resourcePath = ""
      }
    }
    it("should not throw exceptions when implemented with vals") {
      new SwaggerHttpService {
        val actorRefFactory = context
        val apiTypes = Seq(typeOf[DictHttpService])
        val modelTypes = Seq()
        val apiVersion = "1.0"
        val swaggerVersion = "1.2"
        val baseUrl = ""
        val specPath = ""
        val resourcePath = ""
      }
    }
  }

}
