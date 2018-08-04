import Dependencies._

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "net.marsicek",
      scalaVersion := "2.12.6",
      version      := "0.1.0-SNAPSHOT"
    )),
    name := "restaurant-stock",

    resolvers += Resolver.bintrayRepo("hseeberger", "maven"),

    libraryDependencies ++= akkaHttp ++ akkaStream ++ circe ++ akkaHttpCirce ++ scalaTest
  )
