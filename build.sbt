organization := "org.github.vanclist"

name := "vast-service-tests"

version := "0.1.0"

scalaVersion := "2.12.1"

resolvers ++= Seq(
  "justwrote" at "http://repo.justwrote.it/releases/",
  "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
  Resolver.bintrayRepo("hseeberger", "maven"),
  "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"
)

val akkaVersion = "2.4.16"
val akkaHttpVersion = "10.0.3"
val logbackVersion = "1.1.7"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0",
  "ch.qos.logback" % "logback-classic" % logbackVersion,
  "ch.qos.logback" % "logback-core" % logbackVersion,
  "mysql" % "mysql-connector-java" % "5.1.39",
  "org.reactivemongo" %% "reactivemongo" % "0.12.1",
  "com.typesafe.slick" %% "slick" % "3.2.0-M2",
  "com.fasterxml" % "aalto-xml" % "1.0.0",
  "joda-time" % "joda-time" % "2.9.4",
  "org.joda" % "joda-convert" % "1.8.1",
  "org.mongodb" % "bson" % "3.2.2",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test",
  "org.scalacheck" %% "scalacheck" % "1.13.4" % "test",
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test",
  "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
  "de.flapdoodle.embed" % "de.flapdoodle.embed.mongo" % "1.50.5" % "test",
  "org.mockito" % "mockito-core" % "1.10.19" % "test"
)

logBuffered in Test := true

testOptions in Test ++= Seq(
  Tests.Argument(TestFrameworks.ScalaTest, "-oD", "-hD", "target/report"),
  Tests.Argument(TestFrameworks.ScalaCheck, "-minSuccessfulTests", "50", "-workers", "4")
)
