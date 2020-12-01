organization := "nicolaswinsten"
name := "web-viz"
version := "1.0"
scalaVersion := "2.13.4"

// Scala Swing module
libraryDependencies += "org.scala-lang.modules" %% "scala-swing" % "3.0.0"

// Scala scraper for WikiWeb
libraryDependencies += "net.ruippeixotog" %% "scala-scraper" % "2.2.0"

githubTokenSource := TokenSource.GitConfig("github.token")
githubOwner := "NicolasWinsten"
githubRepository := "web"