lazy val root = (project in file(".")).
settings (
  name := "GenericService",
  version := "1.0",
  scalaVersion := "2.13.1",
  scalacOptions in ThisBuild ++= Seq("-unchecked", "-deprecation", "-feature"),
  resolvers += "Typesafe Repository" at "https://repo.typesafe.com/typesafe/releases/",
  libraryDependencies ++= Seq("com.typesafe.akka" %% "akka-actor" % "2.6.4")
)

