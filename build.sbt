ThisBuild / organization := "com.nicolaswinsten"
ThisBuild / version := "1.0.3"
ThisBuild / scalaVersion := "2.13.4"
name := "web-viz"

// Scala Swing module
libraryDependencies += "org.scala-lang.modules" %% "scala-swing" % "3.0.0"
// Scala scraper for WikiWeb
libraryDependencies += "net.ruippeixotog" %% "scala-scraper" % "2.2.0"


githubTokenSource := TokenSource.GitConfig("github.token")
githubOwner := "NicolasWinsten"
githubRepository := "web-viz"

