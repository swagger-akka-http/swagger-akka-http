package com.github.swagger.akka

import java.util.Iterator

import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.`type`.ReferenceType
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import io.swagger.v3.core.converter._
import io.swagger.v3.core.jackson.ModelResolver
import io.swagger.v3.core.util.{Json, PrimitiveType}
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.models.media.{Schema, StringSchema}

class AnnotatedTypeForOption extends AnnotatedType

object SwaggerScalaModelConverter {
  Json.mapper().registerModule(new DefaultScalaModule())
}

class SwaggerScalaModelConverter extends ModelResolver(Json.mapper()) {
  SwaggerScalaModelConverter

  override def resolve(`type`: AnnotatedType, context: ModelConverterContext, chain: Iterator[ModelConverter]): Schema[_] = {
    val javaType = _mapper.constructType(`type`.getType)
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
    val annotatedOverrides = `type` match {
      case _: AnnotatedTypeForOption => Seq.empty
      case _ => {
        nullSafeList(`type`.getCtxAnnotations).collect {
          case p: Parameter => p.required()
        }
      }
    } 
    if (_isOptional(`type`, cls)) {
      val baseType = if (annotatedOverrides.headOption.getOrElse(false)) new AnnotatedType() else new AnnotatedTypeForOption()
      resolve(nextType(baseType, `type`, cls, javaType), context, chain)
    } else if (!annotatedOverrides.headOption.getOrElse(true)) {
      resolve(nextType(new AnnotatedTypeForOption(), `type`, cls, javaType), context, chain)
    } else if (chain.hasNext) {
      val nextResolved = Option(chain.next().resolve(`type`, context, chain))
      nextResolved match {
        case Some(property) => {
          setRequired(`type`)
          property
        }
        case None => null
      }
    } else {
      null
    }
  }

  def _isOptional(annotatedType: AnnotatedType, cls: Class[_]): Boolean = {
    annotatedType.getType match {
      case _: ReferenceType if isOption(cls) => true
      case _ => false
    }
  }

  private def underlyingJavaType(annotatedType: AnnotatedType, cls: Class[_], javaType: JavaType): JavaType = {
    annotatedType.getType match {
      case rt: ReferenceType => rt.getContentType
      case _ => javaType
    }
  }

  private def nextType(baseType: AnnotatedType, `type`: AnnotatedType, cls: Class[_], javaType: JavaType): AnnotatedType = {
    baseType.`type`(underlyingJavaType(`type`, cls, javaType))
      .ctxAnnotations(`type`.getCtxAnnotations)
      .parent(`type`.getParent)
      .schemaProperty(`type`.isSchemaProperty)
      .name(`type`.getName)
      .propertyName(`type`.getPropertyName)
      .resolveAsRef(`type`.isResolveAsRef)
      .jsonViewAnnotation(`type`.getJsonViewAnnotation)
      .skipOverride(true)
  }

  override def _isOptionalType(propType: JavaType): Boolean = {
    isOption(propType.getRawClass) || super._isOptionalType(propType)
  }

  override def _isSetType(cls: Class[_]): Boolean = {
    val setInterfaces = cls.getInterfaces.find { interface =>
      interface == classOf[scala.collection.Set[_]]
    }
    setInterfaces.isDefined || super._isSetType(cls)
  }

  private def setRequired(annotatedType: AnnotatedType): Unit = annotatedType match {
    case _: AnnotatedTypeForOption => // not required
    case _ => {
      Option(annotatedType.getParent).foreach { parent =>
        Option(annotatedType.getPropertyName).foreach { n => parent.addRequiredItem(n) }
      }
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
    else None
  }

  private def isOption(cls: Class[_]): Boolean = cls == classOf[scala.Option[_]]

  def nullSafeList[T](array: Array[T]): List[T] = Option(array) match {
    case None => List[T]()
    case Some(arr) => arr.toList
  }
}
