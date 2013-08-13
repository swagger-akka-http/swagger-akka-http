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
import com.wordnik.swagger.annotations.Api
import com.wordnik.swagger.annotations.ApiImplicitParams
import com.wordnik.swagger.annotations.ApiOperation
import spray.routing.HttpService

class SwaggerApiBuilder(
  swaggerVersion: String,
  apiVersion: String,
  basePath: String,
  apiTypes: Seq[Type],
  modelTypes: Seq[Type]
) {
  
  implicit val mirror = runtimeMirror(getClass.getClassLoader)
  
  private val modelJsonMap = (new SwaggerModelBuilder(modelTypes)).buildAll
  
  val swaggerApiAnnotations = apiTypes.map(apiType => getClassAnnotation[Api](apiType) match {
    case Some(annotation) if !(apiType  <:< typeOf[HttpService]) => 
      throw new IllegalArgumentException(s"Class must mix with HttpServie")
    case Some(annotation) => (annotation, apiType)
    case None => throw new IllegalArgumentException(s"Class must have Api annotation: $apiType")
  })
  
  def buildAll: (ResourceListing, Map[String, ApiListing]) = {
    val listApis = buildResourceListApis(swaggerApiAnnotations)
    val resourceListing = ResourceListing(swaggerVersion, apiVersion, listApis.map(_._1).toList)
    
    val apiListings: Map[String, ApiListing] = (for((listApi, classType) <- listApis) yield {
      (listApi.path, buildApiListing(listApi, classType))
    }).toMap
    
    (resourceListing, apiListings)
  }
  
  private def buildResourceListApis(
    swaggerApiAnnotations: Seq[(Annotation, Type)]
  ): Seq[(ListApi, Type)] = for {
    (apiAnnotation, classType) <- swaggerApiAnnotations 
    apiPath <- getStringJavaAnnotation("value", apiAnnotation)
    description <- getStringJavaAnnotation("description", apiAnnotation)
  } yield {
    (ListApi(apiPath, Some(description), None), classType)
  }
  
  private def buildApiListing(listApi: ListApi, classType: Type): ApiListing = {
     var apis = Map[String, ListApi]()
     var models = Map[String, Model]()
     //Get all methods with an ApiOperation and iterate.
     for {
       (apiOperationAnnotation, termSymbol) <- getAllMethodAnnotations[ApiOperation](classType)
       summary <- getStringJavaAnnotation("value", apiOperationAnnotation)
       httpMethod <- getStringJavaAnnotation("httpMethod", apiOperationAnnotation)
     } {
       //Extract fullpath with and path params and list of optional params
       val (fullPath, params) = getPathAndParams(listApi.path, classType, termSymbol)
       val methodName = termSymbol.name.decoded
       val currentApiOperation = Operation(
           httpMethod = httpMethod,
           summary = summary,
           nickname = getStringJavaAnnotation("nickname", apiOperationAnnotation).getOrElse(methodName),
           responseClass = getClassJavaAnnotation[AnyRef]("response", apiOperationAnnotation).map(_.getSimpleName).getOrElse("void"),
           parameters = params
       )
       //This indicates a new operation for a prexisting api listing, just add it
       if(apis.contains(fullPath)) {
         val prevApi = apis(fullPath)
         val newOperations = currentApiOperation :: prevApi.operations.get 
         apis += fullPath -> prevApi.copy(operations = Some(newOperations))
       //First operation for this type of api listing, create it.
       } else {
  	     apis += fullPath -> ListApi(fullPath, None, Some(List(currentApiOperation)))
       }
    
       models ++= findDependentModels(currentApiOperation.responseClass)
     }
     ApiListing(
       swaggerVersion = swaggerVersion,
       apiVersion = apiVersion,
       basePath = basePath,
       resourcePath = listApi.path,
       apis = apis.values.toList,
       models = if(models.size > 0) Some(models) else None
     )
  }
  
  val SwaggerTypes = List(
      "Byte", "Boolean", "Int", "Long", "Float", "Double", "String", "Date",
      "List", "Set", "Array")
  
  private def findDependentModels(responseClass: String): Map[String, Model] = {
    var models = Map[String, Model]()
    if((responseClass != "void") && modelJsonMap.contains(responseClass)) {
      val model = modelJsonMap(responseClass)
      models += model.id -> model
      
      //Get any models that this model depends upon.
      models ++= findDependentModelsRecursively(model, models)
    }
    models
  }
  
  private def findDependentModelsRecursively(model: Model, models: Map[String, Model]): Map[String, Model] = {
    
    var updatedModels = models
    //Get property based models
    model.properties.values.filter(
      prop => !SwaggerTypes.contains(prop.`type`) && !models.contains(prop.`type`)
    ).foreach(complexProp =>{
      updatedModels += complexProp.`type` -> modelJsonMap(complexProp.`type`)
      updatedModels ++= findDependentModelsRecursively(modelJsonMap(complexProp.`type`), updatedModels)
    })
      //Get all collection models
    val allRefItems = model.properties.values.flatMap(_.items).flatMap(_.get("$ref"))
    allRefItems.filter(key => !models.contains(key)).foreach(refItemId => {
      updatedModels += refItemId -> modelJsonMap(refItemId)
      updatedModels ++= findDependentModelsRecursively(modelJsonMap(refItemId), updatedModels)
    })
    //Get all subclass models
    modelJsonMap.values.filter(m => {
      m.`extends`.exists(e => e == model.id) && !models.contains(m.id)
    }).foreach(m => {
      updatedModels += m.id -> m
      updatedModels ++= findDependentModelsRecursively(m, updatedModels)
    })
    updatedModels
  }
    
  private def getPathAndParams(path: String, classType: Type, termSymbol: Symbol): (String, List[Parameter]) = {
    getMethodAnnotation[ApiImplicitParams](classType)(termSymbol.name.decoded) match {
      case Some(apiParamAnnotation) => 
        getArrayJavaAnnotation("value", apiParamAnnotation) match {
          case Some(annotationParams) =>
            val params = annotationParams.map(annotationParam => Parameter(
  		        name         = getStringJavaAnnotation("name", annotationParam).get, 
    		      description  = getStringJavaAnnotation("value", annotationParam).get, 
    		      `type`       = getStringJavaAnnotation("dataType", annotationParam).get,
    		      paramType    = getStringJavaAnnotation("paramType", annotationParam).get,
    		      required     = getBooleanJavaAnnotation("required", annotationParam).getOrElse(true),
    		      defaultValue = getStringJavaAnnotation("defaultValue", annotationParam)
    		      //allowMultiple = annotationParam.allowMultiple,
    		    ))
            val pathParams = params.filter(_.paramType == "path").map(_.name)
            (pathParams.foldLeft(path)(_ + "/{" + _ + "}"), params.toList)
          case None =>
            (path, List[Parameter]())
         }
      case None => (path, List[Parameter]())
    }
  }
}