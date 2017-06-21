name := "crawler"

version := "1.0"

scalaVersion := "2.12.2"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.5.3",
  "com.typesafe.akka" %% "akka-http" % "10.0.8",
  "org.scalatest" %% "scalatest" % "3.0.3" % Test,
  "org.scalacheck" %% "scalacheck" % "1.13.5" % Test)


scalacOptions := Seq(
  "-unchecked",
  "-deprecation",
  "-Xfatal-warnings"
)

lazy val uberjar = (project in file(".")).settings(
  mainClass in Compile := Some("adamsmo.Crawler"),
  assemblyJarName in assembly := "crawler.jar"
)