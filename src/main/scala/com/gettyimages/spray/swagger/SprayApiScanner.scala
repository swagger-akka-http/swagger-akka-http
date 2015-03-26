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

import scala.reflect.runtime.universe._
import com.wordnik.swagger.annotations.Api
import com.wordnik.swagger.core.SwaggerContext
import com.wordnik.swagger.config._
import com.typesafe.scalalogging.LazyLogging
import spray.routing.HttpService

class SprayApiScanner(apiTypes: Seq[Type])
    extends Scanner
    with LazyLogging {
  def classes(): List[Class[_]] = {

    apiTypes.map(apiType => if (!(apiType <:< typeOf[HttpService])) logger.warn(s"ApiType $apiType does not implement HttpService"))

    apiTypes.collect {
      case api if {
        try {
          SwaggerContext.loadClass(api.toString).getAnnotation(classOf[Api]) != null
        } catch {
          case ex: Exception => {
            logger.error("Problem loading class:  %s. %s: %s".format(api.toString, ex.getMessage))
            false
          }
        }
      } =>
        logger.info("Found API controller:  %s".format(api.toString))
        SwaggerContext.loadClass(api.toString)
    }.toList
  }
}
