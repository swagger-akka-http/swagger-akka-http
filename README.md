# swagger-akka-http

![Build Status](https://github.com/swagger-akka-http/swagger-akka-http/actions/workflows/ci.yml/badge.svg)
[![Gitter](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/swagger-akka-http/Lobby?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.swagger-akka-http/swagger-akka-http_2.13/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.swagger-akka-http/swagger-akka-http_2.13)
[![codecov.io](https://codecov.io/gh/swagger-akka-http/swagger-akka-http/coverage.svg?branch=master)](https://codecov.io/gh/swagger-akka-http/swagger-akka-http/branch/master)

Swagger-Akka-Http brings [Swagger](http://swagger.io/swagger-core/) support for [Akka-Http](http://doc.akka.io/docs/akka-http/current/index.html) Apis. The included `SwaggerHttpService` route will inspect Scala types with Swagger annotations and build a swagger compliant endpoint for a [swagger compliant ui](http://petstore.swagger.io/).

This project was featured in a [blog entry](https://blog.codecentric.de/en/2016/04/swagger-akka-http/) on codecentric.

There is another blog entry and demo hosted by [Knoldus](https://blog.knoldus.com/2017/01/31/document-generation-of-akka-http-using-swagger/).

This is a fork of [gettyimages/spray-swagger](https://github.com/gettyimages/spray-swagger) which has been extended to include pull requests to support the latest swagger.io annotations.
[swagger-spray/swagger-spray](https://github.com/swagger-spray/swagger-spray) is an actively maintained Spray equivalent.

The [swagger spec](http://swagger.io/specification/) is helpful for understanding the swagger api and resource declaration semantics behind swagger-core annotations.

## Getting Swagger-Akka-Http

### Release Version

The jars are hosted on [sonatype](https://oss.sonatype.org) and mirrored to Maven Central. Snapshot releases are also hosted on sonatype. 

Version | Stability | Branch | Description
--------|-----------|--------|------------
2.5.x (SNAPSHOT only) | snapshot | main | Supports Scala 2.13, Akka 2.6.16+, Akka-Http 10.2.6+, Scala-Java8-Compat 1.0+, Swagger 2.1.x libs and OpenAPI 3.0 Specification.
2.4.x | stable | main | Supports Scala 2.12/2.13, Akka 2.5 and 2.6 (prior to 2.6.16), Akka-Http 10.1/10.2, Swagger 2.0/2.1 libs and OpenAPI 3.0 Specification.
1.5.x | stable | swagger-1.5| Supports Scala 2.13, Akka 2.6.16+, Akka-Http 10.2.6+, Scala-Java8-Compat 1.0+, Swagger 1.6.x libs and Swagger 2.0 Specification.
1.4.x | stable | 1.4| Supports  Scala 2.12/2.13, Akka 2.5 and 2.6 (prior to 2.6.16), Akka-Http 10.1/10.2, Swagger 1.5.x/1.6.x libs and Swagger 2.0 Specification.

```sbt
libraryDependencies += "com.github.swagger-akka-http" %% "swagger-akka-http" % "2.4.2"
```
swagger-akka-http 0.10.x and 0.11.x both have had some changes in APIs, for those who are upgrading. See below for details.

Swagger libraries depend heavily on [Jackson](http://wiki.fasterxml.com/JacksonHome). If you need to older versions of Jackson, consider using swagger-akka-http 0.8.2. It depends on Jackson 2.4.

Scala 2.11 support for akka-http 10.1.x requires swagger-akka-http 2.1.1 or 1.1.2.

Scala 2.10 support for akka-http 2.0.3 requires swagger-akka-http 0.6.2.

## Examples

[pjfanning/swagger-akka-http-sample](https://github.com/pjfanning/swagger-akka-http-sample) is a simple sample using this project.

[pjfanning/swagger-akka-http-sample-java](https://github.com/pjfanning/swagger-akka-http-sample-java) demonstrates the experimental Java DSL support in swagger-akka-http 0.10.1.

The `/test` directory includes an `HttpSwaggerServiceSpec` which uses `akka-http-testkit` to test the API. It uses a `PetHttpService` and `UserHttpService` declared in the `/samples` folder. 

## SwaggerHttpService

The `SwaggerHttpService` is a trait extending Akka-Http's `HttpService`. It will generate the appropriate Swagger json schema based on a set of inputs declaring your Api and the types you want to expose.

The `SwaggerHttpService` contains a `routes` property you can concatenate along with your existing akka-http routes. This will expose an endpoint at `<baseUrl>/<specPath>/<resourcePath>` with the specified `apiVersion`, `swaggerVersion` and resource listing.

The service requires a set of `apiTypes` and `modelTypes` you want to expose via Swagger. These types include the appropriate Swagger annotations for describing your api. The `SwaggerHttpService` will inspect these annotations and build the appropriate Swagger response.

Here's an example `SwaggerHttpService` snippet which exposes [Swagger's PetStore](http://petstore.swagger.io/) resources, `Pet`, `User` and `Store`. The routes property can be concatenated to your other route definitions:

```scala
object SwaggerDocService extends SwaggerHttpService {
  override val apiClasses: Set[Class[_]] = Set(classOf[PetService], classOf[UserService], classOf[StoreService])
  override val host = "localhost:8080" //the url of your api, not swagger's json endpoint
  override val apiDocsPath = "api-docs" //where you want the swagger-json endpoint exposed
  override val info = Info() //provides license and other description details
}.routes
```

## Java DSL SwaggerGenerator

This (experimental) support is added in swagger-akka-http 0.10.1. See [pjfanning/swagger-akka-http-sample-java](https://github.com/pjfanning/swagger-akka-http-sample-java) for a demo application.

```java
import com.github.swagger.akka.javadsl.SwaggerGenerator;
class MySwaggerGenerator extends SwaggerGenerator {
  @Override
  public Set<Class<?>> apiClasses() {
    return Collections.singleton(PetService.class);
  }
  
  @Override
  public String host() {
    return "localhost:8080"; //the url of your api, not swagger's json endpoint
  }

  @Override
  public String apiDocsPath() {
    return "api-docs";  //where you want the swagger-json endpoint exposed
  }

  @Override
  public Info info() {
    return new io.swagger.models.Info();  //provides license and other description details
  }
}
```

## Breaking Changes in 0.10.0

In versions prior to 0.10.0, you needed to use code like this:

```scala
class SwaggerDocService(system: ActorSystem) extends SwaggerHttpService with HasActorSystem {
  override implicit val actorSystem: ActorSystem = system
  override implicit val materializer: ActorMaterializer = ActorMaterializer()
  override val apiTypes = Seq(typeOf[PetService], typeOf[UserService], typeOf[StoreService])
  override val host = "localhost:8080" //the url of your api, not swagger's json endpoint
  override val apiDocsPath = "api-docs" //where you want the swagger-json endpoint exposed
  override val info = Info() //provides license and other description details
}.routes
```

* 0.10.0 drops HasActorSystem trait that was not actually useful
* apiClasses has replaced apiTypes
  * In Scala 2.11, you will need to explicitly use the `Set[Class[_]]` type, while Scala 2.12 seems to be able to infer it
* `SwaggerHttpService` now uses `def`s instead of `val`s for more flexibility

## Breaking Changes in 0.11.0

The `val scheme = Scheme.HTTP` has been replaced with `val schemes = List(Scheme.HTTP)`

## Adding Swagger Annotations

Akka-Http routing works by concatenating various routes, built up by directives, to produce an api. The [routing dsl](http://doc.akka.io/docs/akka-http/current/scala/http/introduction.html#routing-dsl-for-http-servers) is an elegant way to describe an api and differs from the more common class and method approach of other frameworks. But because Swagger's annotation library requires classes, methods and fields to describe an Api, one may find it difficult to annotate a akka-http routing application.

A simple solution is to break apart a akka-http routing application into various resource traits, with methods for specific api operations, joined by route concatentation into a route property. These traits with can then be joined together by their own route properties into a complete api. Despite losing the completeness of an entire api the result is a more modular application with a succint resource list. The balance is up to the developer but for a reasonably-sized applicaiton organizing routes across various traits is probably a good idea.

With this structure you can apply `@Api` annotations to these individual traits and `@ApiOperation` annotations to methods.

You can also use jax-rs `@Path` annotations alongside `@ApiOperation`s if you need fine-grained control over path specifications or if you want to support multiple paths per operation. The functionality is the same as swagger-core.

### Resource Definitions

The swagger 2.0 annotations are very different from those used in swagger 1.5.

The general pattern for resource definitions and akka-http routes:

* Place an individual resource in its own trait
* Define specific api operations with `def` methods which produce a route
* Annotate these methods with `@Operation`, `@Parameter` and `@ApiResponse` accordingly
* Concatenate operations together into a single routes property, wrapped with a path directive for that resource
* Concatenate all resource traits together on their routes property to produce the final route structure for your application.

Here's what Swagger's *pet* resource would look like:

```scala
trait PetHttpService extends HttpService {

  @Operation(summary = "Find a pet by ID",
    description = "Returns a pet based on ID",
    method = "GET",
    parameters = Array(
      new Parameter(name = "petId", in = ParameterIn.PATH, required = true, description = "ID of pet that needs to be fetched",
        content = Array(new Content(schema = new Schema(implementation = classOf[Int], allowableValues = Array("[1,100000]")))))
    ),
    responses = Array(
      new ApiResponse(responseCode = "400", description = "Invalid ID supplied"),
      new ApiResponse(responseCode = "404", description = "Pet not found")
    )
  )
  def petGetRoute = get { path("pet" / IntNumber) { petId =>
    complete(s"Hello, I'm pet ${petId}!")
    }
  }
}
```

### Schema Definitions

Schema definitions are fairly self-explanatory. You can use swagger annotations to try to adjust the model generated for a class. Due to type erasure, the `Option[Boolean]` will normally treated as `Option[Any]` but the schema annotation corrects this. This type erasure affects primitives like Int, Long, Boolean, etc.

```scala
case class ModelWOptionBooleanSchemaOverride(@Schema(implementation = classOf[Boolean]) optBoolean: Option[Boolean])
```

## Swagger UI

This library does not include [Swagger's UI](http://petstore.swagger.io/) only the api support for powering a UI. Adding such a UI to your akka-http app is easy with akka-http's `getFromResource` and `getFromResourceDirectory` [support](http://doc.akka.io/docs/akka-http/current/scala/http/routing-dsl/directives/alphabetically.html).

To add a Swagger UI to your site, simply drop the static site files into the resources directory of your project. The following trait will expose a `swagger` route hosting files from the `resources/swagger/` directory: 

```scala
trait Site extends Directives {
  val site =
    path("swagger") { getFromResource("swagger/index.html") } ~
      getFromResourceDirectory("swagger")
}
```

You can then mix this trait with a new or existing Akka-Http class with an `actorRefFactory` and concatenate the `site` route value to your existing route definitions.

## How Annotations are Mapped to Swagger

* [Swagger 2 Annotations Guide](https://github.com/swagger-api/swagger-core/wiki/Swagger-2.X---Annotations)
* [Swagger 1.5 Annotations Guide](https://github.com/swagger-api/swagger-core/wiki/Annotations-1.5.X)
