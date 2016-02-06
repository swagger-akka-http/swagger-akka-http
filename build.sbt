import sbtrelease.ReleasePlugin.ReleaseKeys._

organization := "com.github.swagger-akka-http"

name := "swagger-akka-http"

scalaVersion := "2.11.7"

resolvers ++= Seq(
  "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
  "Maven" at "https://repo1.maven.org/maven2/",
  Resolver.mavenLocal
)

checksums in update := Nil

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http-experimental" % "2.0.3",
  "com.typesafe.akka" %% "akka-http-testkit-experimental" % "2.0.3" % "test",
  "com.typesafe.akka" %% "akka-http-spray-json-experimental" % "2.0.3",
  "io.swagger" %% "swagger-scala-module" % "1.0.1",
  "io.swagger" % "swagger-core" % "1.5.7",
  "io.swagger" % "swagger-annotations" % "1.5.7",
  "io.swagger" % "swagger-models" % "1.5.7",
  "io.swagger" % "swagger-jaxrs" % "1.5.7",
  "org.scalatest" %% "scalatest" % "2.2.6" % "test" ,
  "org.json4s" %% "json4s-jackson" % "3.3.0",
  "org.json4s" %% "json4s-native" % "3.3.0",
  "joda-time" % "joda-time" % "2.9.2" % "test",
  "org.joda" % "joda-convert" % "1.8.1" % "test"
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
    <developer>
      <id>fabiofumarola</id>
      <name>Fabio Fumarola</name>
      <url>https://github.com/fabiofumarola</url>
    </developer>
  </developers>)
