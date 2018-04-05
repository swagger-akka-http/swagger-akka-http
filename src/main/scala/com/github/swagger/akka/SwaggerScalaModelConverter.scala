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
            sp.setRequired(true)
            return sp
          }
        case None =>
          if (cls == classOf[BigDecimal]) {
            val dp = PrimitiveType.DECIMAL.createProperty()
            dp.setRequired(true)
            return dp
          } else if (cls == classOf[BigInt]) {
            val dp = PrimitiveType.INT.createProperty()
            dp.setRequired(true)
            return dp
          }
      }
    }

    // Unbox scala options
    `type` match {
      case rt: ReferenceType if isOption(cls) =>
        val nextType = rt.getContentType
        val nextResolved = {
          Option(resolveProperty(nextType, context, annotations, chain)) match {
            case Some(p) => Some(p)
            case None if chain.hasNext =>
              Option(chain.next().resolveProperty(nextType, context, annotations, chain))
            case _ => None
          }
        }
        nextResolved match {
          case Some(property) =>
            property.setRequired(false)
            property
          case None => null
        }
      case t if chain.hasNext =>
        val nextResolved = Option(chain.next().resolveProperty(t, context, annotations, chain))
        nextResolved match {
          case Some(property) =>
            property.setRequired(true)
            property
          case None => null
        }
      case _ =>
        null
    }
  }

  override
  def resolve(`type`: Type, context: ModelConverterContext, chain: Iterator[ModelConverter]): Model = {
    val javaType = Json.mapper().constructType(`type`)
    getEnumerationInstance(javaType.getRawClass) match {
      case Some(enumInstance) => null // ignore scala enums
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