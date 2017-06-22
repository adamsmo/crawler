name := "crawler"

version := "1.0"

scalaVersion := "2.12.2"

libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "com.github.scopt" %% "scopt" % "3.6.0",
  "org.jsoup" % "jsoup" % "1.10.3",
  "com.typesafe.akka" %% "akka-testkit" % "2.4.19",
  "com.typesafe.akka" %% "akka-actor" % "2.4.19",
  "com.typesafe.akka" %% "akka-http" % "10.0.8",
  "org.scalamock" %% "scalamock-scalatest-support" % "3.6.0" % Test,
  "org.scalatest" %% "scalatest" % "3.0.3" % Test,
  "org.scalacheck" %% "scalacheck" % "1.13.5" % Test)


scalacOptions := Seq(
  "-unchecked",
  "-deprecation",
  "-Xfatal-warnings"
)

lazy val uberjar = (project in file(".")).settings(
  mainClass in Compile := Some("adamsmo.App"),
  assemblyJarName in assembly := "crawler.jar"
)