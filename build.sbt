import sbtrelease.ReleasePlugin.ReleaseKeys._

organization := "com.gettyimages"

name := "spray-swagger"

scalaVersion := "2.10.4"

crossScalaVersions := Seq("2.10.4", "2.11.1")

libraryDependencies ++= { scalaBinaryVersion.value match {
 	case "2.10" => Seq(
    "io.spray"  % "spray-routing" % "1.3.1",
    "io.spray"  % "spray-testkit" % "1.3.1")
	case "2.11" => Seq(
    "io.spray" %% "spray-routing" % "1.3.1-20140423",
    "io.spray" %% "spray-testkit" % "1.3.1-20140423")
  }
 }

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "2.1.5" % "test",
  //"com.wordnik" % "swagger-annotations_2.10" % "1.3.0",
  //"com.wordnik" % "swagger-core_2.10" % "1.3.0",
  "com.wordnik" % "swagger-jaxrs_2.10" % "1.3.5",
  "javax.ws.rs" % "jsr311-api" % "1.1.1",
  "com.typesafe.akka" %% "akka-actor" % "2.3.3",
  "org.json4s" %% "json4s-jackson" % "3.2.9",
  "joda-time" % "joda-time" % "2.2",
  "org.joda" % "joda-convert" % "1.3.1",
  "com.typesafe.scala-logging" %% "scala-logging-slf4j" % "2.1.2"
)

resolvers += "spray repo" at "http://repo.spray.io"

releaseSettings

testOptions in Test += Tests.Argument("-oD")

parallelExecution in Test := false

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

homepage := Some(url("https://github.com/gettyimages/spray-swagger"))

licenses := Seq("The Apache Software License, Version 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt"))

net.virtualvoid.sbt.graph.Plugin.graphSettings

pomExtra := (
  <scm>
    <url>git@github.com:gettyimages/spray-swagger.git</url>
    <connection>scm:git:git@github.com:gettyimages/spray-swagger.git</connection>
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
  </developers>)
