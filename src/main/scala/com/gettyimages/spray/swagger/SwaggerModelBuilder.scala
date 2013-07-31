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
  
  println(modelAnnotationTypesMap)
  
  def build(name: String): Option[Model] = modelAnnotationTypesMap.get(name).map(modelAnnotationType => {
    val (classAnnotation, fieldAnnotationSymbols, modelType) = modelAnnotationType
    val modelProperties = (for((annotation, symbol) <- fieldAnnotationSymbols) yield {
      val description = getStringJavaAnnotation("value", annotation).get
      val modelName = symbol.name.decoded
      val optionType = extractOptionType(symbol)
      val required = getBooleanJavaAnnotation("required", annotation).getOrElse(optionType.isEmpty)
      val (modelTypeName, items) = getModelTypeName(optionType.getOrElse(symbol.typeSignature))
      (modelName, ModelProperty(description = description, required = required, `type` = modelTypeName, items = items.map(Map(_))))
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
  
  private def getModelTypeName(modelType: Type): (String, Option[(String, String)]) = {
    //Container type
    if(modelType <:< typeOf[Seq[_]] || modelType <:< typeOf[Set[_]] || modelType <:< typeOf[Array[_]]) {
      //Doesn't handle nesting
      (if(modelType <:< typeOf[Seq[_]]) "List" else modelType.typeSymbol.name.decoded , 
          Some(getLiteralOrComplexTypeName(modelType.asInstanceOf[TypeRefApi].args.head)))
    //Literal/Complex Type
    } else {
      (getLiteralOrComplexTypeName(modelType)._2, None)
    }
  }
  
  private def getLiteralOrComplexTypeName(modelType: Type): (String, String) = {
    val typeName = modelType.typeSymbol.name.decoded 
    //Literal type
    if (
      modelType =:= typeOf[Byte] || modelType =:= typeOf[Boolean] || modelType =:= typeOf[Int] ||
      modelType =:= typeOf[Long] || modelType =:= typeOf[Float] || modelType =:= typeOf[Double] || 
      modelType =:= typeOf[String] || modelType =:= typeOf[Date]
    ) {
      ("type", typeName)
    //Reference to complex model type
    } else if(modelAnnotationTypesMap.contains(typeName)) {
      ("$ref", typeName)
    //Unknown type
    } else {
      throw new UnsupportedTypeSignature(s"$modelType") 
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