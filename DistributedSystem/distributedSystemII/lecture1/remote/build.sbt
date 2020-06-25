lazy val commonSettings = Seq(
  version := "1.0",
  scalaVersion := "2.13.1",
  scalacOptions in ThisBuild ++= Seq("-unchecked", "-deprecation"),
  resolvers += "Typesafe Repository" at "https://repo.typesafe.com/typesafe/releases/",
  libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.6.4",
  libraryDependencies += "com.typesafe.akka" %% "akka-remote" % "2.6.4"
)

lazy val root = (project in file(".")).
  aggregate(server, client)

lazy val server = (project in file("server")).
  settings(commonSettings: _*).
  settings(
  name := "Server"
)

lazy val client = (project in file("client")).
  settings(commonSettings: _*).
  settings(
  name := "Client"
)

