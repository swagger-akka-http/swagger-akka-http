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

import com.wordnik.swagger.config._
import com.wordnik.swagger.reader._
import com.wordnik.swagger.core.util.ReaderUtil
import com.typesafe.scalalogging.slf4j.LazyLogging
import com.wordnik.swagger.model._
import scala.reflect.runtime.universe._

class SwaggerApiBuilder(
  config: SwaggerConfig,
  apiTypes: Seq[Type],
  modelTypes: Seq[Type]
) extends ReaderUtil
  with LazyLogging {

  val scanner = new SprayApiScanner(apiTypes)
  val reader = new SprayApiReader()
  val listings: Map[String, ApiListing] = {
        logger.info("loading api metadata")
        val classes = scanner match {
          case scanner: Scanner => scanner.classes()
          case _ => List()
        }

        logger.info("classes count:" + classes.length)

        classes.foreach{ clazz =>
          logger.info("class: " + clazz.getName)
        }

        val listings = (for(cls <- classes) yield reader.read("", cls, config)).flatten
        val mergedListings = groupByResourcePath(listings)
        mergedListings.map(m => (m.resourcePath, m)).toMap
    }

  def getApiListing(path: String): Option[ApiListing] = {
    listings.get(path)
  }

  def getResourceListing(): ResourceListing = {

    val references = listings.map {
      case (path, listing) => ApiListingReference(path, listing.description)
    }.toList

    ResourceListing(config.getApiVersion,
                     config.getSwaggerVersion,
                     references, //apilistingreference
                     List(), //authorizations tbd
                     config.info)
  }

}
