package com.gettyimages.spray.swagger

//based on swagger specs - https://github.com/wordnik/swagger-core/wiki/API-Declaration
case class ListApi(path: String, 
					description: Option[String], 
					operations: Option[List[Operation]])

case class ApiListing(swaggerVersion: String, 
						apiVersion: String, 
						basePath: String,
						resourcePath: String,
						apis: List[ListApi],
						models: Option[Map[String, Model]])
					
case class ResourceListing(
    swaggerVersion: String,
    apiVersion: String,
    apis: List[ListApi]
)
 
case class Model(id: String,
                 description: String,
                 properties: Map[String, ModelProperties]) {
}

case class ModelProperties(name: String,
                  description: String,
                  `type`: String,
                  defaultValue: Option[String] = None,
                  enum: List[String] = Nil,
                  required: Boolean = true)

case class Operation(httpMethod: String,
                     responseClass: String,
                     summary: String,
                     notes: Option[String] = None,
                     deprecated: Boolean = false,
                     nickname: Option[String] = None,
                     parameters: List[Parameter] = Nil,
                     errorResponses: List[Error] = Nil)

case class Endpoint(path: String,
                    description: String,
                    secured: Boolean = false,
                    operations: List[Operation] = Nil)

case class Error(code: Int,
                 reason: String)

case class Parameter(name: String,
                 description: String,
                 dataType: String,
                 notes: Option[String] = None,
                 paramType: String,
                 defaultValue: Option[String] = None,                     
                 required: Boolean = true,
                 allowMultiple: Boolean = false)	