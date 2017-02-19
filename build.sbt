name := """event-stream"""

version := "1.0"

scalaVersion := "2.11.8"

// Change this to another test framework if you prefer

//resolvers += Resolver.bintrayRepo("hseeberger", "maven")

//libraryDependencies += "de.heikoseeberger" %% "akka-sse" % "2.0.0"

libraryDependencies += "com.typesafe.akka" % "akka-stream-testkit-experimental_2.11" % "2.0.5"

libraryDependencies += "org.scalatest" % "scalatest_2.11" % "3.2.0-SNAP3"
