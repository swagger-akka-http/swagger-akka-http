organization := "com.github.swagger-akka-http"

name := "swagger-akka-http"

scalaVersion := "2.12.12"

crossScalaVersions := Seq("2.11.12", scalaVersion.value, "2.13.3")

val swaggerVersion = "1.6.2"
val akkaVersion = "2.5.31"
val akkaHttpVersion = "10.1.12"
val jacksonVersion = "2.11.2"
val slf4jVersion = "1.7.30"

checksums in update := Nil

//resolvers += Resolver.sonatypeRepo("snapshots")

Global / useGpg := false

libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-reflect" % scalaVersion.value,
  "org.scala-lang.modules" %% "scala-java8-compat" % "0.9.1",
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion % "test",
  "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % "test",
  "io.swagger" % "swagger-core" % swaggerVersion,
  "io.swagger" % "swagger-annotations" % swaggerVersion,
  "io.swagger" % "swagger-models" % swaggerVersion,
  "io.swagger" % "swagger-jaxrs" % swaggerVersion,
  "com.github.swagger-akka-http" %% "swagger-scala-module" % "1.1.0",
  "org.slf4j" % "slf4j-api" % slf4jVersion,
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % jacksonVersion,
  "com.fasterxml.jackson.dataformat" % "jackson-dataformat-yaml" % jacksonVersion,
  "org.scalatest" %% "scalatest" % "3.0.8" % "test",
  "org.json4s" %% "json4s-native" % "3.6.9" % "test",
  "joda-time" % "joda-time" % "2.10.2" % "test",
  "org.joda" % "joda-convert" % "2.2.1" % "test",
  "org.slf4j" % "slf4j-simple" % slf4jVersion % "test"
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

parallelExecution in Test := false

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
