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

import scala.Option.option2Iterable
import scala.reflect.runtime.universe
import com.wordnik.swagger.annotations.Api
import com.wordnik.swagger.annotations.ApiParamImplicit
import com.wordnik.swagger.annotations.ApiParamsImplicit
import com.wordnik.swagger.annotations.ApiOperation
import spray.routing.Directive.pimpApply
import spray.routing.Route
import spray.routing.directives.CompletionMagnet.fromObject
import scala.reflect.runtime.universe._
import scala.Array.canBuildFrom
import spray.routing.HttpService
import ReflectionUtils._
import org.json4s.Formats
import org.json4s.DefaultFormats
import org.json4s._
import org.json4s.native.JsonMethods._
import spray.httpx.Json4sSupport
import com.typesafe.scalalogging.slf4j.Logging
import com.wordnik.swagger.annotations.ApiClass

trait SwaggerHttpService extends HttpService with Logging with Json4sSupport {
  def apiTypes: Seq[Type]
  def modelTypes: Map[String, Type]
  def apiVersion: String
  def swaggerVersion: String
  def basePath: String 
  def resourcePath: String
  def host: String
  
  implicit def json4sFormats: Formats = DefaultFormats
  
  private val modelJsonMap = (for {
    (name, modelType) <- modelTypes
    classAnnotation <- getClassAnnotation[ApiClass](modelType)
  } yield {
    (name, Model(
      id = name, 
      description = getStringJavaAnnotation("description", classAnnotation).get,
      properties = Map[String, ModelProperties]()
    ))
  }).toMap
  
  final def routes: Route = get { pathPrefix(basePath) {
    val (apiRoutes, listApis) = buildApiRoutes
    path(resourcePath) {
      complete(ResourceListing(
          swaggerVersion, apiVersion, listApis.toList
      ))
    } ~ apiRoutes 
  }}
  
  private def buildResourceListApis(
    swaggerApiAnnotations: Seq[(Annotation, Type)]
  ): Seq[(ListApi, Type)] = for {
    (apiAnnotation, classType) <- swaggerApiAnnotations 
    apiPath <- getStringJavaAnnotation("value", apiAnnotation)
    description <- getStringJavaAnnotation("description", apiAnnotation)
  } yield {
    (ListApi(apiPath, Some(description), None), classType)
  }
    
  private def getPathAndParams(path: String, classType: Type, termSymbol: Symbol): (String, List[Parameter]) = {
    getMethodAnnotation[ApiParamsImplicit](classType)(termSymbol.name.decoded) match {
      case Some(apiParamAnnotation) => 
        getArrayJavaAnnotation("value", apiParamAnnotation) match {
          case Some(annotationParams) =>
            val params = annotationParams.map(annotationParam => Parameter(
		      name        = getStringJavaAnnotation("name", annotationParam).get, 
		      description = getStringJavaAnnotation("value", annotationParam).get, 
		      dataType    = getStringJavaAnnotation("dataType", annotationParam).get,
		      paramType   = getStringJavaAnnotation("paramType", annotationParam).get
		      /*required = annotationParam.required,
		      allowMultiple = annotationParam.allowMultiple,
		      defaultValue = if(annotationParam.defaultValue == "") None else Some(annotationParam.defaultValue)*/
		    ))
            val pathParams = params.filter(_.paramType == "path").map(_.name)
            (pathParams.foldLeft(path)(_ + "/{" + _ + "}"), params.toList)
          case None =>
            (path, List[Parameter]())
         }
      case None => (path, List[Parameter]())
    }
  }
    
  private def buildApiRoute(listApi: ListApi, classType: Type): Route = path(listApi.path.drop(1)) {
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
       val currentApiOperation = Operation(
           httpMethod = httpMethod,
           summary = summary,
           responseClass = getStringJavaAnnotation("responseClass", apiOperationAnnotation).getOrElse("void"),
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
     
       val responseClass: String = currentApiOperation.responseClass
       if((responseClass != "void") && modelJsonMap.contains(responseClass)) {
         val model = modelJsonMap(responseClass)
         models += model.id -> model
       }
     }
     
     complete(ApiListing(
       swaggerVersion = swaggerVersion,
       apiVersion = apiVersion,
       basePath = s"http://${host}/${basePath}",
       resourcePath = listApi.path,
       apis = apis.values.toList,
       models = if(models.size > 0) Some(models) else None
     ))
  }
    
  private def buildApiRoutes: (Route, Seq[ListApi]) = {
    val swaggerApiAnnotations = apiTypes.flatMap(t => getClassAnnotation[Api](t).map(a => (a, t)))
    val listApis = buildResourceListApis(swaggerApiAnnotations)
    
    val route = (for((listApi, classType) <- listApis) yield {
      buildApiRoute(listApi, classType) 
    }).reduceLeft(_ ~ _)
    
    (route, listApis.map(_._1))
  }
}