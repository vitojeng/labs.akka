name := "simple-streams"

version := "1.0"

scalaVersion := "2.13.4"

lazy val akkaVersion = "2.6.11"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "ch.qos.logback" % "logback-classic" % "1.2.3",

  "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test,
  "org.scalatest" %% "scalatest" % "3.1.0" % Test
)
