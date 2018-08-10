package com.github.swagger.akka

import java.util.Iterator

import com.fasterxml.jackson.databind.`type`.ReferenceType
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import io.swagger.v3.core.converter._
import io.swagger.v3.core.util.{Json, PrimitiveType}
import io.swagger.v3.oas.models.media.{Schema, StringSchema}

object SwaggerScalaModelConverter {
  Json.mapper().registerModule(new DefaultScalaModule())
}

class SwaggerScalaModelConverter extends ModelConverter {
  SwaggerScalaModelConverter

  override def resolve(`type`: AnnotatedType, context: ModelConverterContext, chain: Iterator[ModelConverter]): Schema[_] = {
    val javaType = Json.mapper().constructType(`type`.getType)
    val cls = javaType.getRawClass

    if(cls != null) {
      // handle scala enums
      getEnumerationInstance(cls) match {
        case Some(enumInstance) =>
          if (enumInstance.values != null) {
            val sp = new StringSchema()
            for (v <- enumInstance.values)
              sp.addEnumItem(v.toString)
            setRequired(`type`)
            return sp
          }
        case None =>
          if (cls.isAssignableFrom(classOf[BigDecimal])) {
            val dp = PrimitiveType.DECIMAL.createProperty()
            setRequired(`type`)
            return dp
          } else if (cls.isAssignableFrom(classOf[BigInt])) {
            val ip = PrimitiveType.INT.createProperty()
            setRequired(`type`)
            return ip
          }
      }
    }

    // Unbox scala options
    `type`.getType match {
      case rt: ReferenceType if isOption(cls) =>
        val nextType = new AnnotatedType()
          .`type`(rt.getContentType)
          .ctxAnnotations(`type`.getCtxAnnotations)
          .parent(`type`.getParent)
          .schemaProperty(`type`.isSchemaProperty)
          .name(`type`.getName)
          .resolveAsRef(`type`.isResolveAsRef)
          .jsonViewAnnotation(`type`.getJsonViewAnnotation)
          .skipOverride(true)
        context.resolve(nextType)
      case _ if chain.hasNext =>
        val nextResolved = Option(chain.next().resolve(`type`, context, chain))
        nextResolved match {
          case Some(property) => {
            setRequired(`type`)
            property
          }
          case None => null
        }
      case _ =>
        null
    }
  }

  private def setRequired(`type`: AnnotatedType): Unit = {
    (Option(`type`.getParent), Option(`type`.getName)) match {
      case (Some(parent), Some(name)) => parent.addRequiredItem(name)
      case _ =>
    }
  }

  private def getEnumerationInstance(cls: Class[_]): Option[Enumeration] = {
    if (cls.getFields.map(_.getName).contains("MODULE$")) {
      val javaUniverse = scala.reflect.runtime.universe
      val m = javaUniverse.runtimeMirror(Thread.currentThread().getContextClassLoader)
      val moduleMirror = m.reflectModule(m.staticModule(cls.getName))
      moduleMirror.instance match
      {
        case enumInstance: Enumeration => Some(enumInstance)
        case _ => None
      }
    }
    else {
      None
    }
  }

  private def isOption(cls: Class[_]): Boolean = cls == classOf[scala.Option[_]]

}
