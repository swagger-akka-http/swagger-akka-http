organization := "com.github.swagger-akka-http"

name := "swagger-akka-http"

val swaggerVersion = "1.6.2"
val akkaVersion = "2.6.16"
val akkaHttpVersion = "10.2.6"
val jacksonVersion = "2.12.5"
val slf4jVersion = "1.7.32"
val scala213 = "2.13.6"

ThisBuild / scalaVersion := scala213
ThisBuild / crossScalaVersions := Seq(scala213, "2.12.15")

update / checksums := Nil

//resolvers += Resolver.sonatypeRepo("snapshots")

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion % "test",
  "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % "test",
  "io.swagger" % "swagger-core" % swaggerVersion,
  "io.swagger" % "swagger-annotations" % swaggerVersion,
  "io.swagger" % "swagger-models" % swaggerVersion,
  "io.swagger" % "swagger-jaxrs" % swaggerVersion,
  "com.github.swagger-akka-http" %% "swagger-scala-module" % "1.3.0",
  "org.slf4j" % "slf4j-api" % slf4jVersion,
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % jacksonVersion,
  "com.fasterxml.jackson.dataformat" % "jackson-dataformat-yaml" % jacksonVersion,
  "org.scalatest" %% "scalatest" % "3.0.8" % "test",
  "org.json4s" %% "json4s-native" % "3.6.10" % "test",
  "joda-time" % "joda-time" % "2.10.2" % "test",
  "org.joda" % "joda-convert" % "2.2.1" % "test",
  "org.slf4j" % "slf4j-simple" % slf4jVersion % "test"
)

// While not ideal, Akka 2.12 is still on 0.8.0 so to align with them we'll
// stick on 0.8.0 for 2.12 only. This will ensure that users are aligned and
// don't have to add in hacks to avoid the early-semver mismatch that comes if
// you try to include both 1.0.1 and 0.8.0 since it can't safely evict in that
// case.
libraryDependencies += CrossVersion.partialVersion(scalaVersion.value).map {
  case ((2, 12)) => "org.scala-lang.modules" %% "scala-java8-compat" % "0.8.0"
  case _ => "org.scala-lang.modules" %% "scala-java8-compat" % "1.0.1"
}

Test / testOptions += Tests.Argument("-oD")

Test / parallelExecution := false
logBuffered := false

Test / publishArtifact  := false

pomIncludeRepository := { _ => false }

Test / parallelExecution := false

homepage := Some(url("https://github.com/swagger-akka-http/swagger-akka-http"))

licenses := Seq("The Apache Software License, Version 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt"))

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

ThisBuild / githubWorkflowTargetTags ++= Seq("v*")
ThisBuild / githubWorkflowPublishTargetBranches := Seq(
  RefPredicate.Equals(Ref.Branch("main")),
  RefPredicate.Equals(Ref.Branch("swagger-1.5")),
  RefPredicate.StartsWith(Ref.Tag("v"))
)

ThisBuild / githubWorkflowPublish := Seq(
  WorkflowStep.Sbt(
    List("ci-release"),
    env = Map(
      "PGP_PASSPHRASE" -> "${{ secrets.PGP_PASSPHRASE }}",
      "PGP_SECRET" -> "${{ secrets.PGP_SECRET }}",
      "SONATYPE_PASSWORD" -> "${{ secrets.SONATYPE_PASSWORD }}",
      "SONATYPE_USERNAME" -> "${{ secrets.SONATYPE_USERNAME }}"
    )
  )
)
