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
import scala.collection.immutable.ListMap

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
    val modelPropertiesSeq = for((annotation, symbol) <- fieldAnnotationSymbols) yield {
      val description = getStringJavaAnnotation("value", annotation).get
      val dataType = getStringJavaAnnotation("dataType", annotation)
      val propertyName = symbol.name.decoded.trim
      val optionType = extractOptionType(symbol)
      val required = getBooleanJavaAnnotation("required", annotation).getOrElse(optionType.isEmpty)
      val position = getIntJavaAnnotation("position", annotation).getOrElse(0)
      val typeInfo = getModelTypeName(optionType.getOrElse(symbol.typeSignature), dataType)
      (propertyName, ModelProperty(
          description = description,
          required = required,
          `type` = typeInfo.typeName,
          items = typeInfo.itemType.map(ti => Map(ti.typeLabel -> ti.typeName)),
          uniqueItems = if (typeInfo.isUnique) Some(true) else None,
          enum = getEnumValues(getStringJavaAnnotation("allowableValues", annotation), typeInfo),
          position = position
      ))
    }
    val modelProperties = ListMap(modelPropertiesSeq.sortBy(_._2.position): _*)

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
    getClassJavaAnnotation("parent", modelAnnotation) match {
      case Some(c) =>
        Some(c.getSimpleName)
      case _ =>
        modelType.baseClasses.filter(sym => sym != modelType.typeSymbol &&
          getSymbolAnnotation[ApiModel](sym).isDefined).headOption.map(_.name.decoded.trim)
    }
  }

  private def getEnumValues(allowableValuesStr:Option[String], typeInfo: PropertyTypeInfo): Option[Set[String]] = {
    allowableValuesStr match {
      case Some(allowableValues) => Some(allowableValues.split(",").map(_.trim).toSet)
      case None => {
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
  }
  }

  private def getModelTypeName(propertyType: Type, dataTypeOverride: Option[String]): PropertyTypeInfo = {
    dataTypeOverride match {
      // data type was overridden using annotation
      case Some(dataType) =>
        // checks if type is custom and we should ref to it
        def linkType(typeName: String): String =
          if(modelAnnotationTypesMap.contains(typeName)) "$ref" else "type"

        // things like container[something]
        val ComplexTypeMatcher = "([a-zA-Z]*)\\[([a-zA-Z\\.\\-]*)\\].*".r

        dataType match {
          case ComplexTypeMatcher(containerType, itemType) =>
            PropertyTypeInfo(propertyType, "type", containerType,
              itemType = Some(PropertyTypeInfo(propertyType, linkType(itemType), itemType)),
              isUnique = containerType == "set")
          case _ =>
            PropertyTypeInfo(propertyType, linkType(dataType), dataType)
        }

      case None =>
        //Container type
        if(propertyType <:< typeOf[Iterable[_]] || propertyType <:< typeOf[Array[_]]) {
          //Doesn't handle nesting
          val item = propertyType.asInstanceOf[TypeRefApi].args.head
          PropertyTypeInfo(propertyType, "type", "array",
            itemType = getLiteralOrComplexTypeName(item).
              orElse(Some(PropertyTypeInfo(propertyType, "type", item.typeSymbol.fullName))),
            isUnique = propertyType <:< typeOf[Set[_]])
          //Literal/Complex Type
        } else {
          getLiteralOrComplexTypeName(propertyType).getOrElse(
            getModelTypeName(propertyType, Some(propertyType.toString)))
        }
    }
  }

  case class PropertyTypeInfo(
    `type`: Type,
    typeLabel: String,
    typeName: String,
    itemType: Option[PropertyTypeInfo] = None,
    isEnum: Boolean = false,
    isUnique: Boolean = false
  )

  private def getLiteralOrComplexTypeName(propertyType: Type): Option[PropertyTypeInfo] = {
    val typeName = propertyType.typeSymbol.name.decoded
    //Literal type
    if (
      propertyType =:= typeOf[Byte]   || propertyType =:= typeOf[Boolean] || propertyType =:= typeOf[Int] ||
      propertyType =:= typeOf[Long]   || propertyType =:= typeOf[Float]   || propertyType =:= typeOf[Double] ||
      propertyType =:= typeOf[String]
    ) {
      Some(PropertyTypeInfo(propertyType, "type", typeName.toLowerCase))
    } else if(propertyType =:= typeOf[DateTime] || propertyType =:= typeOf[Date]) {
      Some(PropertyTypeInfo(propertyType, "type", "dateTime"))
    //Handle enums
    } else if(propertyType.typeSymbol.fullName == "scala.Enumeration.Value") {
      Some(PropertyTypeInfo(propertyType, "type", "string", isEnum = true))
    //Reference to complex model type
    } else if(modelAnnotationTypesMap.contains(typeName)) {
      Some(PropertyTypeInfo(propertyType, "$ref", typeName))
    //Unknown type
    } else {
      None
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
