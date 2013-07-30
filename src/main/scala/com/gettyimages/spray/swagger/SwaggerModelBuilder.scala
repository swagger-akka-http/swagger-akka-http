package com.gettyimages.spray.swagger

import scala.reflect.runtime.universe._
import ReflectionUtils._
import com.wordnik.swagger.annotations.ApiClass
import java.util.Date
import com.wordnik.swagger.annotations.ApiProperty

class SwaggerModelBuilder(modelTypes: Map[String, Type]) {
  
  def buildAll: Map[String, Model] = (for {
    (name, modelType) <- modelTypes
    classAnnotation <- getClassAnnotation[ApiClass](modelType)
  } yield {
    val fieldAnnotationsSymbols = getAllFieldAnnotations[ApiProperty](modelType)
    val modelProperties = (for((annotation, symbol) <- fieldAnnotationsSymbols) yield {
      val description = getStringJavaAnnotation("value", annotation).get
      val modelName = symbol.name.decoded
      val optionType = extractOptionType(symbol)
      val required = optionType.isEmpty
      val modelType = optionType.getOrElse(symbol.typeSignature)
      val modelTypeName = getModelTypeName(modelType)
      (modelName, ModelProperty(description = description, required = required, `type` = modelType.typeSymbol.name.decoded))
    }).toMap
    
    (name, Model(
      id = name, 
      description = getStringJavaAnnotation("description", classAnnotation).get,
      properties = modelProperties
    ))
  }).toMap
  
  private def getModelTypeName(modelType: Type): (String, Option[(String, String)]) = {
    //Container type
    if(modelType <:< typeOf[Seq[_]] || modelType <:< typeOf[Set[_]] || modelType <:< typeOf[Array[_]]) {
      //Doesn't handle nesting
      (if(modelType <:< typeOf[Seq[_]]) "List" else modelType.typeSymbol.name.decoded , 
          Some(getLiteralOrComplexTypeName(modelType)))
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
    } else if(modelTypes.contains(typeName)) {
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