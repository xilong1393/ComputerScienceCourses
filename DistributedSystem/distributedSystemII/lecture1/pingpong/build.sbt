name := "second"
version := "0.1"
scalaVersion := "2.12.7"
scalacOptions in ThisBuild ++= Seq("-unchecked", "-deprecation"),
resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.6.4"