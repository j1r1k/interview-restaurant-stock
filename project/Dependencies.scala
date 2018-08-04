import sbt._

object Dependencies {
  val akkaHttpVersion = "10.1.3"
  val akkaStreamVersion = "2.5.14"
  val circeVersion = "0.9.3"

  lazy val akkaHttp: Seq[ModuleID] = Seq(
    "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test
  )

  lazy val akkaStream: Seq[ModuleID] = Seq(
    "com.typesafe.akka" %% "akka-stream" % akkaStreamVersion
  )


  lazy val circe: Seq[ModuleID] = Seq(
    "io.circe" %% "circe-core" % circeVersion,
    "io.circe" %% "circe-generic" % circeVersion,
    "io.circe" %% "circe-parser" % circeVersion
  )

  lazy val akkaHttpCirce: Seq[ModuleID] = Seq(
    "de.heikoseeberger" %% "akka-http-circe" % "1.21.0"
  )

  lazy val scalaTest: Seq[ModuleID]  = Seq(
    "org.scalatest" %% "scalatest" % "3.0.5" % Test,
    "org.scalacheck" %% "scalacheck" % "1.14.0" % Test
  )
}
