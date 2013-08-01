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
import com.wordnik.swagger.annotations.ApiClass
import java.util.Date
import com.wordnik.swagger.annotations.ApiProperty

class SwaggerModelBuilder(modelTypes: Seq[Type]) {
 
  //validate models
  val modelAnnotationTypesMap = modelTypes.map(tpe => { getClassAnnotation[ApiClass](tpe) match {
    case Some(annotation) =>
      val value = getStringJavaAnnotation("value", annotation)
      val key = value.getOrElse(tpe.typeSymbol.name.decoded)
      (key, (annotation, getAllFieldAnnotations[ApiProperty](tpe), tpe))
    case None =>
      throw new IllegalArgumentException(
        s"Model does not have ApiClass Annotation $tpe")
  }}).toMap
  
  def build(name: String): Option[Model] = modelAnnotationTypesMap.get(name).map(modelAnnotationType => {
    val (classAnnotation, fieldAnnotationSymbols, modelType) = modelAnnotationType
    val modelProperties = (for((annotation, symbol) <- fieldAnnotationSymbols) yield {
      val description = getStringJavaAnnotation("value", annotation).get
      val modelName = symbol.name.decoded.trim
      val optionType = extractOptionType(symbol)
      val required = getBooleanJavaAnnotation("required", annotation).getOrElse(optionType.isEmpty)
      val typeInfo = getModelTypeName(optionType.getOrElse(symbol.typeSignature))
      val allowableValues = getAllowableValues(modelType, typeInfo)
      (modelName, ModelProperty(
          description = description, 
          required = required, 
          `type` = typeInfo.`type`,
          items = typeInfo.collectionType.map(ti => Map(ti.typeLabel -> ti.`type`)),
          allowableValues = allowableValues
      ))
    }).toMap
    
    Model(
      id = name, 
      description = getStringJavaAnnotation("description", classAnnotation),
      properties = modelProperties
    )
  }) 
  
  def buildAll: Map[String, Model] = modelAnnotationTypesMap.keys.map(name => {
    (name, build(name).get)
  }).toMap
  
  private def getAllowableValues(modelType: Type, typeInfo: SwaggerTypeInfo): Option[AllowableValue] = {
    if(typeInfo.isEnum) {
      //val values = valueSymbols(modelType)
      val values = List[String]()
      Some(AllowableValue.buildList(values)) 
    } else {
      None 
    }
  }
  
  private def getModelTypeName(modelType: Type): SwaggerTypeInfo = {
    //Container type
    if(modelType <:< typeOf[Seq[_]] || modelType <:< typeOf[Set[_]] || modelType <:< typeOf[Array[_]]) {
      //Doesn't handle nesting
      val typeName = if(modelType <:< typeOf[Seq[_]]) "List" else modelType.typeSymbol.name.decoded
      SwaggerTypeInfo("type", typeName, 
          collectionType = Some(getLiteralOrComplexTypeName(modelType.asInstanceOf[TypeRefApi].args.head)))
    //Literal/Complex Type
    } else {
      getLiteralOrComplexTypeName(modelType)
    }
  }
  
  case class SwaggerTypeInfo(
    val typeLabel: String,
    val `type`: String,
    val collectionType: Option[SwaggerTypeInfo] = None,
    val isEnum: Boolean = false
  )
  
  private def getLiteralOrComplexTypeName(modelType: Type): SwaggerTypeInfo = {
    val typeName = modelType.typeSymbol.name.decoded 
    //Literal type
    if (
      modelType =:= typeOf[Byte] || modelType =:= typeOf[Boolean] || modelType =:= typeOf[Int] ||
      modelType =:= typeOf[Long] || modelType =:= typeOf[Float] || modelType =:= typeOf[Double] || 
      modelType =:= typeOf[String] || modelType =:= typeOf[Date]
    ) {
      SwaggerTypeInfo("type", typeName)
    //Handle enums
    } else if(modelType.typeSymbol.fullName == "scala.Enumeration.Value") { 
    //} else if(modelType <:< typeOf[Enumeration.Value]) {
      SwaggerTypeInfo("type", "String", isEnum = true)
    //Reference to complex model type
    } else if(modelAnnotationTypesMap.contains(typeName)) {
      SwaggerTypeInfo("$ref", typeName)
    //Unknown type
    } else {
      throw new UnsupportedTypeSignature(s"$modelType ${modelType.typeSymbol.fullName}") 
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