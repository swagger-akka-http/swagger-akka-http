import org.typelevel.sbt.gha.JavaSpec.Distribution.Zulu
import org.typelevel.sbt.gha.UseRef.Public

organization := "com.github.swagger-akka-http"

name := "swagger-akka-http"

val swaggerVersion = "2.2.10"
val akkaVersion = "2.6.20"
val akkaHttpVersion = "10.2.10"
val jacksonVersion = "2.14.3"
val slf4jVersion = "2.0.7"
val scala213 = "2.13.10"

ThisBuild / scalaVersion := scala213
ThisBuild / crossScalaVersions := Seq(scala213, "2.12.17")

update / checksums := Nil

//resolvers ++= Resolver.sonatypeOssRepos("snapshots")

autoAPIMappings := true

apiMappings ++= {
  def mappingsFor(organization: String, names: List[String], location: String, revision: (String) => String = identity): Seq[(File, URL)] =
    for {
      entry: Attributed[File] <- (Compile / fullClasspath).value
      module: ModuleID <- entry.get(moduleID.key)
      if module.organization == organization
      if names.exists(module.name.startsWith)
    } yield entry.data -> url(location.format(revision(module.revision)))

  val mappings: Seq[(File, URL)] =
    mappingsFor("org.scala-lang", List("scala-library"), "https://scala-lang.org/api/%s/") ++
      mappingsFor("com.typesafe.akka", List("akka-actor", "akka-stream"), "https://doc.akka.io/api/akka/%s/") ++
      mappingsFor("com.typesafe.akka", List("akka-http"), "https://doc.akka.io/api/akka-http/%s/") ++
      mappingsFor("io.swagger.core.v3", List("swagger-core-jakarta"), "https://javadoc.io/doc/io.swagger.core.v3/swagger-core/%s/") ++
      mappingsFor("io.swagger.core.v3", List("swagger-jaxrs2-jakarta"), "https://javadoc.io/doc/io.swagger.core.v3/swagger-jaxrs2/%s/") ++
      mappingsFor("io.swagger.core.v3", List("swagger-models-jakarta"), "https://javadoc.io/doc/io.swagger.core.v3/swagger-models/%s/") ++
      mappingsFor("com.fasterxml.jackson.core", List("jackson-core"), "https://javadoc.io/doc/com.fasterxml.jackson.core/jackson-core/%s/") ++
      mappingsFor("com.fasterxml.jackson.core", List("jackson-databind"), "https://javadoc.io/doc/com.fasterxml.jackson.core/jackson-databind/%s/")

  mappings.toMap
}

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion % Test,
  "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
  "io.swagger.core.v3" % "swagger-core-jakarta" % swaggerVersion,
  "io.swagger.core.v3" % "swagger-annotations-jakarta" % swaggerVersion,
  "io.swagger.core.v3" % "swagger-models-jakarta" % swaggerVersion,
  "io.swagger.core.v3" % "swagger-jaxrs2-jakarta" % swaggerVersion,
  "com.github.swagger-akka-http" %% "swagger-scala-module" % "2.10.0",
  "org.slf4j" % "slf4j-api" % slf4jVersion,
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % jacksonVersion,
  "com.fasterxml.jackson.dataformat" % "jackson-dataformat-yaml" % jacksonVersion,
  "org.scalatest" %% "scalatest" % "3.2.16" % Test,
  "org.json4s" %% "json4s-native" % "4.0.6" % Test,
  "jakarta.ws.rs" % "jakarta.ws.rs-api" % "3.0.0" % Test,
  "joda-time" % "joda-time" % "2.12.5" % Test,
  "org.joda" % "joda-convert" % "2.2.3" % Test,
  "org.slf4j" % "slf4j-simple" % slf4jVersion % Test
)

// While not ideal, Akka 2.12 is still on 0.8.0 so to align with them we'll
// stick on 0.8.0 for 2.12 only. This will ensure that users are aligned and
// don't have to add in hacks to avoid the early-semver mismatch that comes if
// you try to include both 1.0.1 and 0.8.0 since it can't safely evict in that
// case.
libraryDependencies += CrossVersion.partialVersion(scalaVersion.value).map {
  case ((2, 12)) => "org.scala-lang.modules" %% "scala-java8-compat" % "0.8.0"
  case _ => "org.scala-lang.modules" %% "scala-java8-compat" % "1.0.2"
}

Test / testOptions += Tests.Argument("-oD")

Test / publishArtifact := false

Test / parallelExecution := false

pomIncludeRepository := { _ => false }

homepage := Some(url("https://github.com/swagger-akka-http/swagger-akka-http"))

licenses := Seq("The Apache Software License, Version 2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0.txt"))

pomExtra := (
  <developers>
    <developer>
      <id>mhamrah</id>
      <name>Michael Hamrah</name>
      <url>http://michaelhamrah.com</url>
    </developer>
    <developer>
      <id>efuquen</id>
      <name>Edwin Fuquen</name>
      <url>http://parascal.com</url>
    </developer>
    <developer>
      <id>rliebman</id>
      <name>Roberto Liebman</name>
      <url>https://github.com/rleibman</url>
    </developer>
    <developer>
      <id>pjfanning</id>
      <name>PJ Fanning</name>
      <url>https://github.com/pjfanning</url>
    </developer>
  </developers>)

ThisBuild / githubWorkflowBuild := Seq(WorkflowStep.Sbt(List("coverage", "test", "coverageReport")))
ThisBuild / githubWorkflowJavaVersions := Seq(JavaSpec(Zulu, "8"), JavaSpec(Zulu, "11"), JavaSpec(Zulu, "17"))
ThisBuild / githubWorkflowPublishTargetBranches := Seq(
  RefPredicate.Equals(Ref.Branch("main")),
  RefPredicate.Equals(Ref.Branch("swagger-1.5")),
  RefPredicate.StartsWith(Ref.Tag("v"))
)

ThisBuild / githubWorkflowBuildPostamble := Seq(
  WorkflowStep.Use(Public("codecov", "codecov-action", "v2"), Map("fail_ci_if_error" -> "true"))
)

ThisBuild / githubWorkflowPublish := Seq(
  WorkflowStep.Sbt(
    List("ci-release"),
    env = Map(
      "PGP_PASSPHRASE" -> "${{ secrets.PGP_PASSPHRASE }}",
      "PGP_SECRET" -> "${{ secrets.PGP_SECRET }}",
      "SONATYPE_PASSWORD" -> "${{ secrets.SONATYPE_PASSWORD }}",
      "SONATYPE_USERNAME" -> "${{ secrets.SONATYPE_USERNAME }}",
      "CI_SNAPSHOT_RELEASE" -> "+publishSigned"
    )
  )
)
