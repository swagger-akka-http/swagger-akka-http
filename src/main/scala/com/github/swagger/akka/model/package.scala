/*
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
package com.github.swagger.akka

import scala.collection.JavaConverters._
import scala.compat.java8.OptionConverters._
import scala.language.implicitConversions
import io.swagger.models.{
  Info ⇒ SwaggerInfo,
  Contact ⇒ SwaggerContact,
  License ⇒ SwaggerLicense
}

/**
 * @author rleibman
 */
package object model {
  case class Contact(name: String, url: String, email: String)

  case class License(name: String, url: String)

  case class Info(description: String = "",
                   version: String = "",
                   title: String = "",
                   termsOfService: String = "",
                   contact: Option[Contact] = None,
                   license: Option[License] = None,
                   vendorExtensions: Map[String, Object] = Map.empty)

  implicit def swagger2scala(convertMe: SwaggerContact): Option[Contact] = {
    if (convertMe == null) None else Some(Contact(convertMe.getName, convertMe.getUrl, convertMe.getEmail))
  }
  implicit def scala2swagger(convertMe: Contact): SwaggerContact = {
    if (convertMe == null) {
      null
    } else {
      new SwaggerContact()
        .name(convertMe.name)
        .url(convertMe.url)
        .email(convertMe.email)
    }
  }
  implicit def swagger2scala(convertMe: SwaggerLicense): Option[License] = {
    if (convertMe == null) None else Some(License(convertMe.getName, convertMe.getUrl))
  }
  implicit def scala2swagger(convertMe: License): SwaggerLicense = {
    if (convertMe == null) {
      null
    } else {
      new SwaggerLicense()
        .name(convertMe.name)
        .url(convertMe.url)
    }
  }
  implicit def swagger2scala(convertMe: SwaggerInfo): Info = {
    Info(convertMe.getDescription,
      convertMe.getVersion,
      convertMe.getTitle,
      convertMe.getTermsOfService,
      convertMe.getContact,
      convertMe.getLicense,
      convertMe.getVendorExtensions.asScala.toMap)
  }
  implicit def scala2swagger(convertMe: Info): SwaggerInfo = {
    val ret = new SwaggerInfo()
      .description(convertMe.description)
      .version(convertMe.version)
      .title(convertMe.title)
      .termsOfService(convertMe.termsOfService)
      .contact(convertMe.contact.getOrElse(null))
      .license(convertMe.license.getOrElse(null))

    ret.getVendorExtensions.putAll(convertMe.vendorExtensions.asJava)
    ret
  }
  def asScala[K,V](jmap: java.util.Map[K,V]): Map[K,V] = Option(jmap) match {
    case None => Map.empty[K,V]
    case Some(jm) => jm.asScala.toMap
  }
  def asScala[T](jlist: java.util.List[T]): List[T] = Option(jlist) match {
    case None => List.empty[T]
    case Some(jl) => jl.asScala.toList
  }
  def asScala[T](jset: java.util.Set[T]): Set[T] = Option(jset) match {
    case None => Set.empty[T]
    case Some(js) => js.asScala.toSet
  }
  def asScala[T](jopt: java.util.Optional[T]): Option[T] = Option(jopt) flatMap { opt => opt.asScala }
}
