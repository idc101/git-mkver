import Dependencies._

ThisBuild / scalaVersion     := "2.12.11"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.example"
ThisBuild / organizationName := "example"

lazy val root = (project in file("."))
  .settings(
    name := "git-mkver",
    //libraryDependencies += "com.github.xuwei-k" %% "optparse-applicative" % "0.8.2",
    libraryDependencies += "org.typelevel" %% "cats-core" % "2.0.0",
    libraryDependencies += "com.monovore" %% "decline" % "1.0.0",
    libraryDependencies += scalaTest % Test
  )
