organization := "com.gettyimages"

name := "spray-swagger"

version := "0.1-SNAPSHOT"

scalaVersion := "2.10.2"

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "2.0.M5b" % "test",
  "com.wordnik" % "swagger-core_2.10.0" % "1.2.5",
  "com.typesafe" %% "scalalogging-slf4j" % "1.0.1",
  "com.typesafe.akka" %% "akka-actor" % "2.1.4",
  "org.json4s" %% "json4s-native" % "3.2.2",
  "io.spray" % "spray-routing" % "1.1-M8"
)

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
