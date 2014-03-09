/**
 * Copyright 2013 Getty Imges, Inc.
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
import ReflectionUtils._
import com.wordnik.swagger.annotations.ApiModel
import java.util.Date
import com.wordnik.swagger.annotations.ApiModelProperty
import org.joda.time.DateTime
import com.typesafe.scalalogging.slf4j.Logging

class SwaggerModelBuilder(modelTypes: Seq[Type])(implicit mirror: Mirror) extends Logging {
  
  //validate models
  val modelAnnotationTypesMap = modelTypes.map(tpe => { getClassAnnotation[ApiModel](tpe) match {
    case Some(annotation) =>
      val value = getStringJavaAnnotation("value", annotation)
      val key = value.getOrElse(tpe.typeSymbol.name.decoded)
      (key, (annotation, getAllFieldAnnotations[ApiModelProperty](tpe), tpe))
    case None =>
      throw new IllegalArgumentException(
        s"Model does not have ApiClass Annotation $tpe")
  }}).toMap
  
  logger.debug(s"ModelAnnotationTypesMap: $modelAnnotationTypesMap")
  
  def build(name: String): Option[Model] = modelAnnotationTypesMap.get(name).map(modelAnnotationType => {
    val (modelAnnotation, fieldAnnotationSymbols, modelType) = modelAnnotationType
    val extendedName = getExtendedClassName(modelType, modelAnnotation) 
    val subTypes = getSubTypes(modelType, modelAnnotation)
    val modelProperties = (for((annotation, symbol) <- fieldAnnotationSymbols) yield {
      val description = getStringJavaAnnotation("value", annotation).get
      val dataType = getStringJavaAnnotation("dataType", annotation)
      val propertyName = symbol.name.decoded.trim
      val optionType = extractOptionType(symbol)
      val required = getBooleanJavaAnnotation("required", annotation).getOrElse(optionType.isEmpty)
      val typeInfo = getModelTypeName(optionType.getOrElse(symbol.typeSignature))
      (propertyName, ModelProperty(
          description = description, 
          required = required, 
          `type` = dataType.getOrElse(typeInfo.typeName),
          items = typeInfo.collectionType.map(ti => Map(ti.typeLabel -> ti.typeName)),
          enum = getEnumValues(typeInfo)
      ))
    }).toMap
    
    Model(
      id = name, 
      description = getStringJavaAnnotation("description", modelAnnotation),
      `extends` = extendedName,
      subTypes = subTypes,
      properties = modelProperties
    )
  })
  
  def buildAll: Map[String, Model] = modelAnnotationTypesMap.keys.map(name => {
    (name, build(name).get)
  }).toMap
  
  private def getSubTypes(modelType: Type, modelAnnotation: Annotation): Option[List[String]] = {
    getArrayClassJavaAnnotation("subTypes", modelAnnotation) match {
      case Some(subTypeClasses) => 
        Some(subTypeClasses.map(_.getSimpleName).toList)
      case None =>
        val subTypes = modelType.typeSymbol.asClass.knownDirectSubclasses.map(_.name.decoded.trim).toList
        if(subTypes.size > 0) Some(subTypes) else None
    }
  }
  
  private def getExtendedClassName(modelType: Type, modelAnnotation: Annotation): Option[String] = {
    getStringJavaAnnotation("extends", modelAnnotation) orElse {
      modelType.baseClasses.filter(sym => sym != modelType.typeSymbol && 
        getSymbolAnnotation[ApiModel](sym).isDefined).headOption.map(_.name.decoded.trim)
    }
  }
  
  private def getEnumValues(typeInfo: PropertyTypeInfo): Option[Set[String]] = {
    if(typeInfo.isEnum) {
      val enumType = getOuterType(typeInfo.`type`)
      val enumObj = getCompanionObject(enumType)
      val enumMirror = mirror.reflect(enumObj)
      val values = valueSymbols(typeInfo.`type`).map(valueSymbol => {
        val valueObj = enumMirror.reflectField(valueSymbol.asTerm).get
        val valueMirror = mirror.reflect(valueObj)
        val toStringMethodSymbol = valueSymbol.typeSignature.member("toString": TermName).asMethod
        val toStringMethodMirror = valueMirror.reflectMethod(toStringMethodSymbol)
        
        toStringMethodMirror().asInstanceOf[String]
      }).toList
      Some(values.toSet) 
    } else {
      None 
    }
  }
  
  private def getModelTypeName(propertyType: Type): PropertyTypeInfo = {
    //Container type
    if(propertyType <:< typeOf[Iterable[_]] || propertyType <:< typeOf[Array[_]]) {
      //Doesn't handle nesting
      val typeName = if(propertyType <:< typeOf[Seq[_]]) "List" else propertyType.typeSymbol.name.decoded
      PropertyTypeInfo(propertyType, "type", typeName, 
          collectionType = Some(getLiteralOrComplexTypeName(propertyType.asInstanceOf[TypeRefApi].args.head)))
    //Literal/Complex Type
    } else {
      getLiteralOrComplexTypeName(propertyType)
    }
  }
  
  case class PropertyTypeInfo(
    val `type`: Type,
    val typeLabel: String, 
    val typeName: String, 
    val collectionType: Option[PropertyTypeInfo] = None, 
    val isEnum: Boolean = false
  )
  
  private def getLiteralOrComplexTypeName(propertyType: Type): PropertyTypeInfo = {
    val typeName = propertyType.typeSymbol.name.decoded 
    //Literal type
    if (
      propertyType =:= typeOf[Byte]   || propertyType =:= typeOf[Boolean] || propertyType =:= typeOf[Int] ||
      propertyType =:= typeOf[Long]   || propertyType =:= typeOf[Float]   || propertyType =:= typeOf[Double] || 
      propertyType =:= typeOf[String] || propertyType =:= typeOf[Date]
    ) {
      PropertyTypeInfo(propertyType, "type", typeName)
    } else if(propertyType =:= typeOf[DateTime]) {
      PropertyTypeInfo(propertyType, "type", "Date")
    //Handle enums
    } else if(propertyType.typeSymbol.fullName == "scala.Enumeration.Value") { 
    //} else if(modelType <:< typeOf[Enumeration.Value]) {
      PropertyTypeInfo(propertyType, "type", "String", isEnum = true)
    //Reference to complex model type
    } else if(modelAnnotationTypesMap.contains(typeName)) {
      PropertyTypeInfo(propertyType, "$ref", typeName)
    //Unknown type
    } else {
      PropertyTypeInfo(propertyType, "type", propertyType.typeSymbol.fullName)
    }
  }
  
  private def extractOptionType(symbol: TermSymbol): Option[Type] = symbol.typeSignature match {
    case TypeRef(_, tpe, args) =>
      if(tpe == typeOf[Option[_]].asInstanceOf[ExistentialTypeApi].typeSymbol) {
        Some(args.head)
      } else {
        None
      }
    case _ => None
  }

  case class UnsupportedTypeSignature(msg: String) extends Exception(msg)
}
