# spray-swagger

Spray-Swagger brings [Swagger](https://github.com/wordnik/swagger-core) support for [Spray](http://spray.io) Apis. The included ```SwaggerHttpService``` route will inspect Scala types with Swagger annotations and build a swagger compliant endpoint for a [swagger compliant ui](https://github.com/wordnik/swagger-ui).

The swagger spec [swagger spec](https://github.com/wordnik/swagger-spec/blob/master/versions/1.2.md) is helpful for understanding the swagger api and resource declaration semantics behind swagger-core annotations.

## Getting Spray-Swagger

### Release Version

The jars are hosted on [sonatype](https://oss.sonatype.org). Starting with 0.4.0-SNAPSHOT 
spray-swagger is cross-compiled with scala 2.10.4 and 2.11.1. Snapshot releases are also hosted on sonatype. 

```
libraryDependencies += "com.gettyimages" %% "spray-swagger" % "0.4.2"
```

## SwaggerHttpService

The ```SwaggerHttpService``` is a trait extending Spray's ```HttpService```. It will generate the appropriate Swagger json schema based on a set of inputs declaring your Api and the types you want to exposed.

The  ```SwagerHttpService``` will contain a ```routes``` property you can concatenate along with your existing spray routes. This will expose an endpoint at ```<baseUrl>/<specPath>/<resourcePath>``` with the specified ```apiVersion```, ```swaggerVersion``` and resource listing.

The service requires a set of ```apiTypes``` and ```modelTypes``` you want to expose via Swagger. These types include the appropriate Swagger annotations for describing your api. The ```SwaggerHttpService``` will inspect these annotations and build the approrpiate Swagger response.

Here's an example ```SwaggerHttpService``` snippet which exposes [Wordnik's PetStore](http://swagger.wordnik.com/) resources, ```Pet```, ```User``` and ```Store```. The routes property can be concatenated to your other route definitions:

```
new SwaggerHttpService {
       def actorRefFactory = context
       def apiTypes = Seq(typeOf[PetService], typeOf[UserService], typeOf[StoreService])
       def apiVersion = "1.0"
       def swaggerVersion = "1.2" // you can omit, defaults to 1.2
       def baseUrl = "http://localhost:8080" //the url of your api
       def docsPath = "/api-docs" //where you want the swagger-json endpoint exposed
     }.routes
```

## Adding Swagger Annotations

Spray-routing works by concatenating various routes, built up by directives, to produce an api. The [routing dsl](http://spray.io/documentation/1.2.1/spray-routing/) is an elegant way to describe an api and differs from the more common class and method approach of other frameworks. But because Swagger's annotation library requires classes, methods and fields to describe an Api, one may find it difficult to annotate a spray-routing application.

A simple solution is to break apart a spray-routing application into various resource traits, with methods for specific api operations, joined by route concatentation into a route property. These traits with can then be joined together by their own route properties into a complete api. Despite losing the completeness of an entire api the result is a more modular application with a succint resource list. The balance is up to the developer but for a reasonably-sized applicaiton organizing routes across various traits is probably a good idea.

With this structure you can apply ```@Api``` annotations to these individual traits and ```@ApiOperation``` annotations to methods.

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

This library does not include [Swagger's UI](https://github.com/wordnik/swagger-ui) only the api support for powering a UI. Adding such a UI to your Spray app is easy with Spray's ```getFromResource``` and ```getFromResourceDirectory``` support.

To add a Swagger UI to your site, simply drop the static site files into the resources directory of your project. The following trait will expose a ```swagger``` route hosting files from the ```resources/swagger/`` directory: 

```
trait Site extends HttpService {
  val site =
    path("swagger") { getFromResource("swagger/index.html") } ~
      getFromResourceDirectory("swagger")
}
```

You can then mix this trait with a new or existing Spray class with an ``actorRefFactory``` and concatenate the ```site``` route value to your existing route definitions.

## Examples

The ```/test``` directory includes an ```HttpSwaggerServiceSpec``` which leverages ```spray.testkit``` to test the API. It uses a ```PetHttpService``` and ```UserHttpService``` declared in the ```/samples``` folder. 
