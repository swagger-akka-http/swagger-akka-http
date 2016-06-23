import sbtrelease.ReleasePlugin.ReleaseKeys._

organization := "com.github.swagger-akka-http"

name := "swagger-akka-http"

scalaVersion := "2.11.8"

coverageEnabled := true
coverageHighlighting := {
  if (scalaBinaryVersion.value == "2.10") false
  else false
}

resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

resolvers += "Maven" at "https://repo1.maven.org/maven2/"

resolvers += Resolver.mavenLocal

checksums in update := Nil

libraryDependencies ++= Seq(
  "com.fasterxml.jackson.core" % "jackson-core" % "2.7.5",
  "com.fasterxml.jackson.core" % "jackson-annotations" % "2.7.5",
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.7.5",
  "com.fasterxml.jackson.dataformat" % "jackson-dataformat-xml" % "2.7.5",
  "com.fasterxml.jackson.dataformat" % "jackson-dataformat-yaml" % "2.7.5",
  "com.fasterxml.jackson.datatype" % "jackson-datatype-joda" % "2.7.5",
  "com.fasterxml.jackson.jaxrs" % "jackson-jaxrs-base" % "2.7.5",
  "com.fasterxml.jackson.jaxrs" % "jackson-jaxrs-json-provider" % "2.7.5",
  "com.fasterxml.jackson.module" % "jackson-module-jaxb-annotations" % "2.7.5",
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.7.4",

  "com.typesafe.akka" %% "akka-http-experimental" % "2.4.7",
  "com.typesafe.akka" %% "akka-http-testkit" % "2.4.7" % "test",
  "com.typesafe.akka" %% "akka-http-spray-json-experimental" % "2.4.7",
  "io.swagger" %% "swagger-scala-module" % "1.0.2",
  "io.swagger" % "swagger-core" % "1.5.9",
  "io.swagger" % "swagger-annotations" % "1.5.9",
  "io.swagger" % "swagger-models" % "1.5.9",
  "io.swagger" % "swagger-jaxrs" % "1.5.9",
  "org.scalatest" %% "scalatest" % "2.2.5" % "test",
  "org.json4s" %% "json4s-jackson" % "3.4.0",
  "org.json4s" %% "json4s-native" % "3.4.0",
  "joda-time" % "joda-time" % "2.8" % "test",
  "org.joda" % "joda-convert" % "1.7" % "test",
  "org.slf4j" % "slf4j-simple" % "1.7.7" % "test"
)


releaseSettings

testOptions in Test += Tests.Argument("-oD")

parallelExecution in Test := false
logBuffered := false

publishMavenStyle := true

publishTo <<= version { (v: String) =>
  val nexus = "https://oss.sonatype.org/"
  if (v.trim.endsWith("SNAPSHOT"))
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

parallelExecution in Test := false

homepage := Some(url("https://github.com/swagger-akka-http/swagger-akka-http"))

licenses := Seq("The Apache Software License, Version 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt"))

publishArtifactsAction := PgpKeys.publishSigned.value

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
