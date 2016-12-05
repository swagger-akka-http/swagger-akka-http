/*
Copyright 2016 SmartBear Software, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package com.github.swagger.akka

import java.lang.annotation.Annotation
import java.lang.reflect.Type
import java.util.Iterator

import com.fasterxml.jackson.databind.`type`.ReferenceType
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import io.swagger.converter._
import io.swagger.models.Model
import io.swagger.models.properties._
import io.swagger.util.{Json, PrimitiveType}

object SwaggerScalaModelConverter {
  Json.mapper().registerModule(new DefaultScalaModule())
}

class SwaggerScalaModelConverter extends ModelConverter {
  SwaggerScalaModelConverter

  override
  def resolveProperty(`type`: Type, context: ModelConverterContext,
    annotations: Array[Annotation] , chain: Iterator[ModelConverter]): Property = {
    val javaType = Json.mapper().constructType(`type`)
    val cls = javaType.getRawClass

    if(cls != null) {
      // handle scala enums
      getEnumerationInstance(cls) match {
        case Some(enumInstance) =>
          if (enumInstance.values != null) {
            val sp = new StringProperty()
            for (v <- enumInstance.values)
              sp._enum(v.toString)
            return sp
          }
        case None =>
          if (cls.isAssignableFrom(classOf[BigDecimal])) {
            return PrimitiveType.DECIMAL.createProperty()
          }
      }
    }

    // Unbox scala options
    val nextType = `type` match {
        case rt: ReferenceType if isOption(cls) => rt.getContentType
        case _ => `type`
      }

    if (chain.hasNext())
      chain.next().resolveProperty(nextType, context, annotations, chain)
    else
      null
    }

  override
  def resolve(`type`: Type, context: ModelConverterContext, chain: Iterator[ModelConverter]): Model = {
    val javaType = Json.mapper().constructType(`type`)
    getEnumerationInstance(javaType.getRawClass) match {
      case Some(enumInstance) =>null // ignore scala enums
      case None =>
        if (chain.hasNext()) {
          val next = chain.next()
          next.resolve(`type`, context, chain)
        }
        else
          null
    }
  }
  private def getEnumerationInstance(cls: Class[_]): Option[Enumeration] =
  {
    if (cls.getFields.map(_.getName).contains("MODULE$"))
    {
      val javaUniverse = scala.reflect.runtime.universe
      val m = javaUniverse.runtimeMirror(Thread.currentThread().getContextClassLoader)
      val moduleMirror = m.reflectModule(m.staticModule(cls.getName))
      moduleMirror.instance match
      {
        case enumInstance: Enumeration => Some(enumInstance)
        case _ => None
      }
    }
    else{
      None
    }
  }

  private def isOption(cls: Class[_]): Boolean = cls == classOf[scala.Option[_]]

}
