organization := "com.github.swagger-akka-http"

name := "swagger-akka-http"

scalaVersion := "2.13.3"

crossScalaVersions := Seq("2.11.12", "2.12.11", scalaVersion.value)

val swaggerVersion = "2.1.3"
val akkaVersion = "2.5.31"
val akkaHttpVersion = "10.1.12"
val jacksonVersion = "2.11.1"
val slf4jVersion = "1.7.30"

checksums in update := Nil

resolvers += Resolver.sonatypeRepo("snapshots")

Global / useGpg := false

libraryDependencies ++= Seq(
  "org.scala-lang.modules" %% "scala-java8-compat" % "0.9.1",
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion % Test,
  "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
  "io.swagger.core.v3" % "swagger-core" % swaggerVersion,
  "io.swagger.core.v3" % "swagger-annotations" % swaggerVersion,
  "io.swagger.core.v3" % "swagger-models" % swaggerVersion,
  "io.swagger.core.v3" % "swagger-jaxrs2" % swaggerVersion,
  "com.github.swagger-akka-http" %% "swagger-scala-module" % "2.1.3",
  "org.slf4j" % "slf4j-api" % slf4jVersion,
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % jacksonVersion,
  "com.fasterxml.jackson.dataformat" % "jackson-dataformat-yaml" % jacksonVersion,
  "org.scalatest" %% "scalatest" % "3.2.0" % Test,
  "org.json4s" %% "json4s-native" % "3.6.9" % Test,
  "javax.ws.rs" % "javax.ws.rs-api" % "2.0.1" % Test,
  "joda-time" % "joda-time" % "2.10.6" % Test,
  "org.joda" % "joda-convert" % "2.2.1" % Test,
  "org.slf4j" % "slf4j-simple" % slf4jVersion % Test
)

testOptions in Test += Tests.Argument("-oD")

parallelExecution in Test := false
logBuffered := false

publishMavenStyle := true

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

homepage := Some(url("https://github.com/swagger-akka-http/swagger-akka-http"))

licenses := Seq("The Apache Software License, Version 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt"))

releasePublishArtifactsAction := PgpKeys.publishSigned.value

pomExtra := (
  <scm>
    <url>git@github.com:swagger-akka-http/swagger-akka-http.git</url>
    <connection>scm:git:git@github.com:swagger-akka-http/swagger-akka-http.git</connection>
  </scm>
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
