organization := "com.github.swagger-akka-http"

name := "swagger-akka-http"

scalaVersion := "2.11.8"

resolvers += "jitpack" at "https://jitpack.io"

val swaggerVersion = "v1.5.10.2"
val akkaHttpVersion = "10.0.0"
val jacksonVersion = "2.8.4"
val slf4jVersion = "1.7.7"

checksums in update := Nil

EclipseKeys.withSource := true

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion % "test",
  "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % "test",
  "com.github.pjfanning.swagger-core" % "swagger-core" % swaggerVersion,
  "com.github.pjfanning.swagger-core" % "swagger-annotations" % swaggerVersion,
  "com.github.pjfanning.swagger-core" % "swagger-models" % swaggerVersion,
  "com.github.pjfanning.swagger-core" % "swagger-jaxrs" % swaggerVersion,
  "org.slf4j" % "slf4j-api" % slf4jVersion,
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % jacksonVersion,
  "com.fasterxml.jackson.dataformat" % "jackson-dataformat-yaml" % jacksonVersion,
  "org.scalatest" %% "scalatest" % "3.0.1" % "test",
  "org.json4s" %% "json4s-native" % "3.4.2" % "test",
  "joda-time" % "joda-time" % "2.8" % "test",
  "org.joda" % "joda-convert" % "1.7" % "test",
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
