import Dependencies._

scalaVersion := "2.12.11"
version := "1.4.0+ci-build.2fd9c7f"
organization := "net.cardnell"
maintainer := "idc101@users.noreply.github.com"

enablePlugins(JavaAppPackaging)

lazy val root = (project in file("."))
  .settings(
        name := "git-mkver",
        scalacOptions += "-Ypartial-unification",
        libraryDependencies += "org.typelevel" %% "cats-core" % "2.0.0",
        libraryDependencies += "com.monovore" %% "decline" % "1.0.0",
        libraryDependencies += "dev.zio" %% "zio" % "1.0.3",
        libraryDependencies += "dev.zio" %% "zio-process" % "0.1.0",
        libraryDependencies += "dev.zio" %% "zio-config" % "1.0.0-RC27",
        libraryDependencies += "dev.zio" %% "zio-config-typesafe" % "1.0.0-RC27",
        libraryDependencies += "dev.zio" %% "zio-test"     % "1.0.3" % Test,
        libraryDependencies += "dev.zio" %% "zio-test-sbt" % "1.0.3" % Test,

        testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),

        assemblyMergeStrategy in assembly := {
              case PathList("META-INF", xs @ _*) => MergeStrategy.discard
              case x => MergeStrategy.first
        }
  )
