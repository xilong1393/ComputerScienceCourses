lazy val commonSettings = Seq(
  version := "1.0",
  scalaVersion := "2.13.1",
  scalacOptions in ThisBuild ++= Seq("-unchecked", "-deprecation"),
  resolvers += "Typesafe Repository" at "https://repo.typesafe.com/typesafe/releases/",
  libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.6.4",
  libraryDependencies += "com.typesafe.akka" %% "akka-remote" % "2.6.4"
)

lazy val root = (project in file(".")).
  aggregate(server2, server1, client)

lazy val server2 = (project in file("server2")).
  settings(commonSettings: _*).
  settings(
  name := "Server 2"
)

lazy val server1 = (project in file("server1")).
  settings(commonSettings: _*).
  settings(
  name := "Server 1"
)

lazy val client = (project in file("client")).
  settings(commonSettings: _*).
  settings(
  name := "Client"
)

