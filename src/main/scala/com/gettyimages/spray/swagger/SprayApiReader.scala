package com.gettyimages.spray.swagger

import com.wordnik.swagger.jaxrs.{MutableParameter, JaxrsApiReader}
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
import javax.ws.rs._

class SprayApiReader
  extends JaxrsApiReader
  with LazyLogging {

  override
  def readRecursive(
    docRoot: String,
    parentPath: String, cls: Class[_],
    config: SwaggerConfig,
    operations: ListBuffer[Tuple3[String, String, ListBuffer[Operation]]],
    parentMethods: ListBuffer[Method]): Option[ApiListing] = {
      Option(cls.getAnnotation(classOf[Api])).map(api => {
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
/* mlh no path option operations, for now
          val path = method.getAnnotation(classOf[Path]) match {
            case e: Path => e.value()
            case _ => ""
          }*/


          if(method.getAnnotation(classOf[ApiOperation]) != null) {
              readMethod(method) match {
                case Some(op) => {
                  val path: String = op.parameters.filter(_.paramType == "path").map(_.name).foldLeft("")(_ + "/{" + _ + "}")
                  appendOperation(addLeadingSlash(api.value) + path, "", op, operations)
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

        ApiListing (
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
          position = api.position)

      })
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

  @Deprecated
  override def readMethod(method: Method, parentParams : List[Parameter], parentMethods : ListBuffer[Method]) : Option[Operation] = {
   // don't use this - it is specific to Jax-RS models.
    throw new RuntimeException("method not in use..")
    None
  }
}

