import sbtrelease.ReleasePlugin.ReleaseKeys._

organization := "com.gettyimages"

name := "spray-swagger"

scalaVersion := "2.10.3"

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "2.1.0" % "test",
  "com.wordnik" %% "swagger-core" % "1.3.2",
  "com.typesafe" %% "scalalogging-slf4j" % "1.0.1",
  "com.typesafe.akka" %% "akka-actor" % "2.3.0",
  "org.json4s" %% "json4s-jackson" % "3.2.4",
  "io.spray" % "spray-routing" % "1.3.0",
  "joda-time" % "joda-time" % "2.2",
  "org.joda" % "joda-convert" % "1.3.1",
  "com.typesafe" %% "scalalogging-slf4j" % "1.0.1"
)

resolvers += "spray repo" at "http://repo.spray.io"

releaseSettings

testOptions in Test += Tests.Argument("-oDF")

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

usePgpKeyHex("4B3A4DF39903AA31")

homepage := Some(url("https://github.com/gettyimages/spray-swagger"))

licenses := Seq("The Apache Software License, Version 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt"))

pomExtra := (
  <scm>
    <url>git@github.com:gettyimages/spray-swagger.git</url>
    <connection>scm:git:git@github.com:gettyimages/spray-swagger.git</connection>
  </scm>
  <developers>
    <developer>
      <id>efuquen</id>
      <name>Edwin Fuquen</name>
      <url>http://parascal.com</url>
    </developer>
  </developers>)
