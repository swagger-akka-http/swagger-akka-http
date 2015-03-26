import sbtrelease.ReleasePlugin.ReleaseKeys._

organization := "com.gettyimages"

name := "spray-swagger"

scalaVersion := "2.11.6"

libraryDependencies ++= Seq(
  "io.spray" %% "spray-routing" % "1.3.3",
  "io.spray" %% "spray-testkit" % "1.3.3" % "test",
  "org.scalatest" %% "scalatest" % "2.2.4" % "test" ,
  "com.wordnik" %% "swagger-core" % "1.3.12" excludeAll( ExclusionRule(organization = "org.json4s"),  ExclusionRule(organization="org.fasterxml*") ),
  "com.typesafe.akka" %% "akka-actor" % "2.3.9",
  "org.json4s" %% "json4s-jackson" % "3.2.11",
  "org.json4s" %% "json4s-native" % "3.2.11",
  "joda-time" % "joda-time" % "2.7",
  "org.joda" % "joda-convert" % "1.7",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",
  "javax.ws.rs" % "jsr311-api" % "1.1.1"
)

resolvers += "spray repo" at "http://repo.spray.io"

releaseSettings

crossBuild := true

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

publishArtifactsAction := PgpKeys.publishSigned.value

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
