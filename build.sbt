import Dependencies._

ThisBuild / scalaVersion := "2.12.11"
ThisBuild / version := "0.4.0"
ThisBuild / organization := "net.cardnell"

lazy val root = (project in file("."))
  .settings(
      name := "git-mkver",
      scalacOptions += "-Ypartial-unification",
      libraryDependencies += "org.typelevel" %% "cats-core" % "2.0.0",
      libraryDependencies += "com.monovore" %% "decline" % "1.0.0",
      libraryDependencies += "com.github.pathikrit" %% "better-files" % "3.8.0",
      libraryDependencies += "dev.zio" %% "zio-config" % "1.0.0-RC14",
      libraryDependencies += "dev.zio" %% "zio-config-typesafe" % "1.0.0-RC14",
      libraryDependencies += scalaTest % Test,

      assemblyMergeStrategy in assembly := {
            case PathList("META-INF", xs @ _*) => MergeStrategy.discard
            case x => MergeStrategy.first
      }
  )
