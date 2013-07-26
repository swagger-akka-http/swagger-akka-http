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

trait SwaggerHttpService extends HttpService with Logging with Json4sSupport {
  def apiTypes: Seq[Type]
  def apiVersion: String
  def swaggerVersion: String
  def basePath: String 
  def resourcePath: String
  def host: String
  
  implicit def json4sFormats: Formats = DefaultFormats
  
  final def routes: Route = get { pathPrefix(basePath) {
    val (apiRoutes, listApis) = buildApiRoutes
    path(resourcePath) {
      complete(ResourceListing(
          swaggerVersion, apiVersion, listApis.toList
      ))
    } ~ apiRoutes 
  }}
  
  private def buildApiRoutes: (Route, Seq[ListApi]) = {
    println(apiTypes)
    val swaggerApiAnnotations = apiTypes.flatMap(t => getClassAnnotation[Api](t).map(a => (a, t)))
    val listApis = (for {
      (apiAnnotation, classType) <- swaggerApiAnnotations 
      apiPath <- getStringJavaAnnotation("value", apiAnnotation)
      description <- getStringJavaAnnotation("description", apiAnnotation)
    } yield {
        (ListApi(apiPath, Some(description), None), classType)
    })
    
    val route = (for((listApi, classType) <- listApis) yield {
       path(listApi.path.drop(1)) {
        
         var apis = Map[String, ListApi]()
         for {
           (apiOperationAnnotation, termSymbol) <- getAllMethodAnnotations[ApiOperation](classType)
           path <- getStringJavaAnnotation("value", apiOperationAnnotation)
           notes <- getStringJavaAnnotation("notes", apiOperationAnnotation)
         } {
           val (fullPath, params) = getMethodAnnotation[ApiParamsImplicit](classType)(termSymbol.name.decoded) match {
             case Some(apiParamAnnotation) => 
               getArrayJavaAnnotation[ApiParamImplicit]("value", apiParamAnnotation) match {
                 case Some(params) =>
	               val pathParams = params.filter(_.paramType == "path").map(_.name)
	               (pathParams.foldLeft(path)(_ + "/{" + _ + "}"), params)
                 case None =>
                   (path, Array[ApiParamImplicit]())
               }
             case None => (path, Array[ApiParamImplicit]())
           }
           if(apis.contains(fullPath)) {
             val prevApi = apis(fullPath)
             //TODO: Should be different
             val newOperations = prevApi.operations
             apis += fullPath -> prevApi.copy(operations = newOperations)
           } else {
             //TODO: Should be different
		     apis += fullPath -> ListApi(fullPath, None, None)
           }
         }
         
         complete(ApiListing(
           swaggerVersion = swaggerVersion,
           apiVersion = apiVersion,
           basePath = s"http://${host}/${basePath}",
           resourcePath = listApi.path,
           apis = List[ListApi](),
           models = None
         ))
       }    
    }).reduceLeft(_ ~ _)
    
    (route, listApis.map(_._1))
  }
}
