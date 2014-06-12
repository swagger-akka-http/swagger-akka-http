/**
 * Copyright 2014 Getty Imges, Inc.
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

import com.wordnik.swagger.config._
import com.wordnik.swagger.model._
import com.wordnik.swagger.annotations._
import scala.collection.mutable.ListBuffer
import java.lang.reflect.Method
import com.typesafe.scalalogging.slf4j.LazyLogging
import java.lang.annotation.Annotation
import scala.reflect.runtime.universe.Type
import com.wordnik.swagger.core.ApiValues._
import com.wordnik.swagger.core.util._
import com.wordnik.swagger.core._
import javax.ws.rs._
import com.wordnik.swagger.reader._

class SprayApiReader
  extends ClassReader
  with ClassReaderUtils
  with LazyLogging {

  def readRecursive(
    docRoot: String,
    parentPath: String,
    cls: Class[_],
    config: SwaggerConfig,
    operations: ListBuffer[Tuple3[String, String, ListBuffer[Operation]]],
    parentMethods: ListBuffer[Method]): Option[ApiListing] = {
      Option(cls.getAnnotation(classOf[Api])) match {
        case None => throw new IllegalArgumentException(s"Class must have Api annotation: @Api")
        case Some(api) =>
          val consumes = Option(api.consumes) match {
            case Some(e) if(e != "") => e.split(",").map(_.trim).toList
            case _ => cls.getAnnotation(classOf[Consumes]) match {
              case e: Consumes => e.value.toList
              case _ => List()
            }
          }
          val produces = Option(api.produces) match {
            case Some(e) if(e != "") => e.split(",").map(_.trim).toList
            case _ => cls.getAnnotation(classOf[Produces]) match {
              case e: Produces => e.value.toList
              case _ => List()
            }
          }
          val protocols = Option(api.protocols) match {
            case Some(e) if(e != "") => e.split(",").map(_.trim).toList
            case _ => List()
          }
          val description = api.description match {
            case e: String if(e != "") => Some(e)
            case _ => None
          }

          // define a Map to hold Operations keyed by resourcepath
          for (method <- cls.getMethods) {

            if(method.getAnnotation(classOf[ApiOperation]) != null) {
                readMethod(method) match {
                  case Some(op) => {
                    val path = method.getAnnotation(classOf[Path]) match {
                      case e: Path => e.value()
                      case _ => op.parameters.filter(_.paramType == "path").map(_.name).foldLeft("")(_ + "/{" + _ + "}")
                    }
                    val opWithName = op.nickname match {
                      case "" => op.copy(nickname = method.getName)
                      case other => op
                    }
                    appendOperation(addLeadingSlash(api.value) + path, "", opWithName, operations)
                  }
                  case None =>
     }
              }
          }

          // sort them by min position in the operations
          val s = (for(op <- operations) yield {
            (op, op._3.map(_.position).toList.min)
          }).sortWith(_._2 < _._2).toList

          val orderedOperations = new ListBuffer[Tuple3[String, String, ListBuffer[Operation]]]
          s.foreach(op => {
            val ops = op._1._3.sortWith(_.position < _.position)
            orderedOperations += Tuple3(op._1._1, op._1._2, ops)
          })

          val apis = (for ((endpoint, resourcePath, operationList) <- orderedOperations) yield {
            val orderedOperations = new ListBuffer[Operation]
              operationList.sortWith(_.position < _.position).foreach(e => orderedOperations += e)
              ApiDescription(
                addLeadingSlash(endpoint),
                None,
                orderedOperations.toList)
            }).toList

          val models = ModelUtil.modelsFromApis(apis)
          Some(ApiListing(
            apiVersion = config.apiVersion,
            swaggerVersion = config.swaggerVersion,
            basePath = config.basePath,
            resourcePath = addLeadingSlash(api.value),
            apis = ModelUtil.stripPackages(apis),
            models = models,
            description = description,
            produces = produces,
            consumes = consumes,
            protocols = protocols,
            position = api.position))

        }
  }
  //mlh probably refactor this away
  def readString(value: String, defaultValue: String = null, ignoreValue: String = null): String = {
    if (defaultValue != null && defaultValue.trim.length > 0) defaultValue
    else if (value == null) null
    else if (value.trim.length == 0) null
    else if (ignoreValue != null && value.equals(ignoreValue)) null
    else value.trim
  }

  def appendOperation(endpoint: String, path: String, op: Operation, operations: ListBuffer[Tuple3[String, String, ListBuffer[Operation]]]) = {
    operations.filter(op => op._1 == endpoint) match {
      case e: ListBuffer[Tuple3[String, String, ListBuffer[Operation]]] if(e.size > 0) => e.head._3 += op
      case _ => operations += Tuple3(endpoint, path, new ListBuffer[Operation]() ++= List(op))
    }
  }

   def read(docRoot: String, cls: Class[_], config: SwaggerConfig): Option[ApiListing] = {
    val parentPath = {
      Option(cls.getAnnotation(classOf[Path])) match {
        case Some(e) => e.value()
        case _ => ""
      }
    }
    readRecursive(docRoot, parentPath.replace("//","/"), cls, config, new ListBuffer[Tuple3[String, String, ListBuffer[Operation]]], new ListBuffer[Method])
  }

  def processResponsesAnnotation(responseAnnotations: ApiResponses) = {
    if (responseAnnotations == null) List()
    else (for (response <- responseAnnotations.value) yield {
      val apiResponse = {
        if (response.response != classOf[Void])
          Some(response.response.getName)
        else None
      }
      ResponseMessage(response.code, response.message, apiResponse)
    }).toList
  }

  def readMethod(method: Method): Option[Operation] = {
    val apiOperation = method.getAnnotation(classOf[ApiOperation])

    if (method.getAnnotation(classOf[ApiOperation]) != null) {
      logger.debug("annotation: ApiOperation: %s,".format(apiOperation.toString))

      val produces = apiOperation.produces match {
        case e: String if e.trim != "" => e.split(",").map(_.trim).toList
        case _ => List()
      }

      val consumes = apiOperation.consumes match {
        case e: String if e.trim != "" => e.split(",").map(_.trim).toList
        case _ => List()
      }
      val protocols = apiOperation.protocols match {
        case e: String if e.trim != "" => e.split(",").map(_.trim).toList
        case _ => List()
      }
      val authorizations:List[com.wordnik.swagger.model.Authorization] = Option(apiOperation.authorizations) match {
        case Some(e) => (for(a <- e) yield {
          val scopes = (for(s <- a.scopes) yield com.wordnik.swagger.model.AuthorizationScope(s.scope, s.description)).toArray
          new com.wordnik.swagger.model.Authorization(a.value, scopes)
        }).toList
        case _ => List()
      }
      val responseClass = apiOperation.responseContainer match {
        case "" => apiOperation.response.getName
        case e: String => "%s[%s]".format(e, apiOperation.response.getName)
      }

      val responseAnnotations = method.getAnnotation(classOf[ApiResponses])

      val apiResponses = processResponsesAnnotation(responseAnnotations)

      val isDeprecated = Option(method.getAnnotation(classOf[Deprecated])).map(m => "true").getOrElse(null)

      val implicitParams = processImplicitParams(method)

      val params = processParams(method)

      Some(Operation(
        apiOperation.httpMethod,
        apiOperation.value,
        apiOperation.notes,
        responseClass,
        apiOperation.nickname,
        apiOperation.position,
        produces,
        consumes,
        protocols,
        authorizations,
        params ++ implicitParams,
        apiResponses,
        Option(isDeprecated)))
    } else {
      None
    }
  }

 def processImplicitParams(method: Method) = {
    logger.debug("checking for ApiImplicitParams")
    Option(method.getAnnotation(classOf[ApiImplicitParams])) match {
      case Some(e) => {
        (for (param <- e.value) yield {
          logger.debug("processing " + param)
          val allowableValues = toAllowableValues(param.allowableValues)
          if(param.dataType == "" || param.dataType == null) {
            logger.error("An implicit parameter was set without a dataType. You must explicitly set the dataType")
          }

          Parameter(
            param.name,
            Option(readString(param.value)),
            Option(param.defaultValue).filter(_.trim.nonEmpty),
            param.required,
            param.allowMultiple,
            param.dataType,
            allowableValues,
            param.paramType,
            Option(param.access).filter(_.trim.nonEmpty))
        }).toList
      }
      case _ => List()
    }
  }

  def processParams(method: Method): List[Parameter] = {
    logger.debug("checking for ApiParams")
    val paramAnnotations = method.getParameterAnnotations
    val paramTypes = method.getParameterTypes
    val genericParamTypes: Array[java.lang.reflect.Type] = method.getGenericParameterTypes

    val paramList = new ListBuffer[Parameter]
    for ((annotations, paramType, genericParamType) <- (paramAnnotations, paramTypes, genericParamTypes).zipped.toList) yield {
      if (annotations.length > 0) {
        val param = new MutableParameter
        param.dataType = processDataType(paramType, genericParamType)
        paramList ++= processParamAnnotations(param, annotations)
      }
    }

    paramList.toList
  }

  def findSubresourceType(method: Method): Class[_] = {
    method.getReturnType
  }

  def processParamAnnotations(mutable: MutableParameter, paramAnnotations: Array[Annotation]): List[Parameter] = {
    List()
  }

  def addLeadingSlash(e: String): String = {
    e.startsWith("/") match {
      case true => e
      case false => "/" + e
    }
  }
  val GenericTypeMapper = "([a-zA-Z\\.]*)<([a-zA-Z0-9\\.\\,\\s]*)>".r

  def processDataType(paramType: Class[_], genericParamType: java.lang.reflect.Type) = {
      paramType.getName match {
        case "[I" => "Array[int]"
        case "[Z" => "Array[boolean]"
        case "[D" => "Array[double]"
        case "[F" => "Array[float]"
        case "[J" => "Array[long]"
        case _ => {
          if(paramType.isArray) {
            "Array[%s]".format(paramType.getComponentType.getName)
          }
          else {
            genericParamType.toString match {
              case GenericTypeMapper(container, base) => {
                val qt = SwaggerTypes(base.split("\\.").last) match {
                  case "object" => base
                  case e: String => e
                }
                val b = ModelUtil.modelFromString(qt) match {
                  case Some(e) => e._2.qualifiedType
                  case None => qt
                }
                "%s[%s]".format(normalizeContainer(container), b)
              }
              case _ => paramType.getName
            }
          }
        }
      }
    }

  //mlh fugly, but necessary for now
  class MutableParameter(param: Parameter) {

    var name: String = _
    var description: Option[String] = None
    var defaultValue: Option[String] = None
    var required: Boolean = _
    var allowMultiple: Boolean = false
    var dataType: String = _
    var allowableValues: AllowableValues = AnyAllowableValues
    var paramType: String = _
    var paramAccess: Option[String] = None

    if(param != null) {
      this.name = param.name
      this.description = param.description
      this.defaultValue = param.defaultValue
      this.required = param.required
      this.allowMultiple = param.allowMultiple
      this.dataType = param.dataType
      this.allowableValues = param.allowableValues
      this.paramType = param.paramType
      this.paramAccess = param.paramAccess
    }

    def this() = this(null)

    def asParameter() = {
      Parameter(name,
        description,
        defaultValue,
        required,
        allowMultiple,
        dataType,
        allowableValues,
        paramType,
        paramAccess)
    }
  }
  def normalizeContainer(str: String) = {
    if(str.indexOf(".List") >= 0) "List"
    else if(str.indexOf(".Set") >= 0) "Set"
    else {
      println("UNKNOWN TYPE: " + str)
      "UNKNOWN"
    }
  }
}

