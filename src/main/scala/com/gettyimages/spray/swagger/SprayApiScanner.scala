package com.gettyimages.spray.swagger

import scala.reflect.runtime.universe.Type
import com.wordnik.swagger.annotations.Api
import com.wordnik.swagger.core.SwaggerContext
import com.wordnik.swagger.config._
import com.typesafe.scalalogging.slf4j.LazyLogging

class SprayApiScanner(apiTypes: Seq[Type])
extends Scanner
with LazyLogging {
  def classes(): List[Class[_]] = {

    apiTypes.collect {
      case api if {
        try {
          SwaggerContext.loadClass(api.toString).getAnnotation(classOf[Api]) != null
        } catch {
          case ex: Exception => {
            logger.error("Problem loading class:  %s. %s: %s".format(api.toString, ex.getMessage))
        false}
          }
      } =>
        logger.info("Found API controller:  %s".format(api.toString))
        SwaggerContext.loadClass(api.toString)
      }.toList
  }
}
