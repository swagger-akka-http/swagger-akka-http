# swagger-spray

[![Build Status](https://travis-ci.org/swagger-spray/swagger-spray.svg?branch=master)](https://travis-ci.org/swagger-spray/swagger-spray)

Swagger-Spray brings [Swagger](http://swagger.io/swagger-core/) support for [Spray](http://spray.io) Apis. The included ```SwaggerHttpService``` route will inspect Scala types with Swagger annotations and build a swagger compliant endpoint for a [swagger compliant ui](http://petstore.swagger.io/).

This is a fork of https://github.com/gettyimages/spray-swagger which has been extended to include pull requests to support the latest swagger.io annotations.
https://github.com/swagger-akka-http/swagger-akka-http is an actively maintained Akka-Http equivalent.

The swagger spec [swagger spec](http://swagger.io/specification/) is helpful for understanding the swagger api and resource declaration semantics behind swagger-core annotations.

## Getting Swagger-Spray

### Release Version

The jars are hosted on [sonatype](https://oss.sonatype.org) and mirrored to Maven Central. Swagger-spray is built against scala 2.10 and 2.11. Snapshot releases are also hosted on sonatype. 

```
libraryDependencies += "com.github.swagger-spray" %% "swagger-spray" % "0.6.2"
```

## Examples

https://github.com/pjfanning/swagger-spray-sample is a simple sample based on this project.

[mhamrah/spray-swagger-sample](https://github.com/mhamrah/spray-swagger-sample) is a spray api project with the original spray-swagger support and a Swagger UI.

The ```/test``` directory includes an ```HttpSwaggerServiceSpec``` which leverages ```spray.testkit``` to test the API. It uses a ```PetHttpService``` and ```UserHttpService``` declared in the ```/samples``` folder. 

## SwaggerHttpService

The ```SwaggerHttpService``` is a trait extending Spray's ```HttpService```. It will generate the appropriate Swagger json schema based on a set of inputs declaring your Api and the types you want to expose.

The  ```SwagerHttpService``` will contain a ```routes``` property you can concatenate along with your existing spray routes. This will expose an endpoint at ```<baseUrl>/<specPath>/<resourcePath>``` with the specified ```apiVersion```, ```swaggerVersion``` and resource listing.

The service requires a set of ```apiTypes``` you want to expose via Swagger. These types include the appropriate Swagger annotations for describing your api. The ```SwaggerHttpService``` will inspect these annotations and build the appropriate Swagger response.

Here's an example ```SwaggerHttpService``` snippet which exposes [Swagger's PetStore](http://petstore.swagger.io/) resources, ```Pet```, ```User``` and ```Store```. The routes property can be concatenated to your other route definitions:

```
new SwaggerHttpService {
       implicit def actorRefFactory = context
       override val apiTypes = Seq(typeOf[PetService], typeOf[UserService], typeOf[StoreService])
       override val host = "localhost:8080" //the url of your api, not swagger's json endpoint
       override val basePath = "/"    //the basePath for the API you are exposing
       override val apiDocsPath = "api-docs" //where you want the swagger-json endpoint exposed
       val info = Info() //provides license and other description details
     }.routes
```

## Adding Swagger Annotations

Spray-routing works by concatenating various routes, built up by directives, to produce an api. The [routing dsl](http://spray.io/documentation/1.2.2/spray-routing/) is an elegant way to describe an api and differs from the more common class and method approach of other frameworks. But because Swagger's annotation library requires classes, methods and fields to describe an Api, one may find it difficult to annotate a spray-routing application.

A simple solution is to break apart a spray-routing application into various resource traits, with methods for specific api operations, joined by route concatentation into a route property. These traits with can then be joined together by their own route properties into a complete api. Despite losing the completeness of an entire api the result is a more modular application with a succint resource list. The balance is up to the developer but for a reasonably-sized applicaiton organizing routes across various traits is probably a good idea.

With this structure you can apply ```@Api``` annotations to these individual traits and ```@ApiOperation``` annotations to methods.

You can also use jax-rs ```@Path``` annotations alongside ```@ApiOperation```s if you need fine-grained control over path specifications or if you want to support multiple paths per operation. The functionality is the same as swagger-core.

### Resource Definitions

The general pattern for resource definitions and spray routes:

* Place an individual resource in its own trait
* Annotate the trait with ```@Api``` to describe the resource
* Define specific api operations with ```def``` methods which produce a route
* Annotate these methods with ```@ApiOperation```, ```@ApiImplictParams``` and ```@ApiResponse``` accordingly
* Concatenate operations together into a single routes property, wrapped with a path directive for that resource
* Concatenate all resource traits together on their routes property to produce the final route structure for your application.

Here's what Swagger's *pet* resource would look like:

```
@Path("/pet")
@Api(value = "/pet", description = "Operations about pets")
trait PetHttpService extends HttpService {

  @ApiOperation(httpMethod = "GET", response = classOf[Pet], value = "Returns a pet based on ID")
  @ApiImplicitParams(Array(
      new ApiImplicitParam(name = "petId", required = false, dataType = "integer", paramType = "path", value = "ID of pet that needs to be fetched")
        ))
  @ApiResponses(Array(
    new ApiResponse(code = 400, message = "Invalid ID Supplied"),
    new ApiResponse(code = 404, message = "Pet not found")))
  def petGetRoute = get { path("pet" / IntNumber) { petId =>
    complete(s"Hello, I'm pet ${petId}!")
    } }
}
```

Notice the use of ```ApiImplicitParams```. This is the best way to apply parameter information. The ```paramType``` can be used to specify ```path```, ```body```, ```header```, ```query``` or ```form```. If the dataType value is not of the basic types, ```spray-swagger``` will try and find the type in the ```modelTypes``` sequence. Refer to *swagger-core* for other attribute information.

### Model Definitions

Model definitions are fairly self-explanatory. Attributes are applied to case class entities and their respective properties. A simplified Pet model:

```
@ApiModel(description = "A pet object")
case class Pet(
  @(ApiModelProperty @field)(value = "unique identifier for the pet")
  val id: Int,

  @(ApiModelProperty @field)(value = "The name of the pet")
  val name: String)
```

## Swagger UI

This library does not include [Swagger's UI](http://petstore.swagger.io/) only the api support for powering a UI. Adding such a UI to your Spray app is easy with Spray's ```getFromResource``` and ```getFromResourceDirectory``` support.

To add a Swagger UI to your site, simply drop the static site files into the resources directory of your project. The following trait will expose a ```swagger``` route hosting files from the ```resources/swagger/`` directory: 

```
trait Site extends HttpService {
  val site =
    path("swagger") { getFromResource("swagger/index.html") } ~
      getFromResourceDirectory("swagger")
}
```

You can then mix this trait with a new or existing Spray class with an ``actorRefFactory``` and concatenate the ```site``` route value to your existing route definitions.

## How Annotations are Mapped to Swagger

[Swagger Annotations Guide](https://github.com/swagger-api/swagger-core/wiki/Annotations-1.5.X)
