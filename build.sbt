import sbt.Keys._

ThisBuild / organization := "com.web-viz"
ThisBuild / scalaVersion := "2.13.4"
ThisBuild / version := "1.0"

lazy val root = (project in file(".")).settings(
  name := "web"
)

// HTML parser backed by Jsoup
ThisBuild / libraryDependencies += "net.ruippeixotog" %% "scala-scraper" % "2.2.0"
// Scala Swing module
ThisBuild / libraryDependencies += "org.scala-lang.modules" %% "scala-swing" % "3.0.0"

//val baseScalacOptions = Seq(
//  "-deprecation",
//  "-unchecked",
//  "-language:implicitConversions"
//)