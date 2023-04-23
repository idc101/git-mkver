package net.cardnell.mkver

import zio.test.Assertion._
import zio.test.mock.Expectation._
import zio.test.mock._
import zio.test.{DefaultRunnableSpec, assertM, suite, testM}
import zio.{Has, ULayer, URLayer, ZLayer}
import Main.mainImpl
import zio.console.Console

// TODO >> @Mockable[Git.Service]
object GitMock extends Mock[Git] {
  object CurrentBranch extends Effect[Unit, Nothing, String]
  object FullLog extends Effect[Option[String], Nothing, String]
  object CommitInfoLog extends Effect[Unit, Nothing, String]
  object Tag extends Effect[(String, String), Nothing, Unit]
  object CheckGitRepo extends Effect[Unit, Nothing, Unit]

  val compose: URLayer[Has[Proxy], Git] =
    ZLayer.fromService { proxy =>
      new Git.Service {
        def currentBranch() = proxy(CurrentBranch)
        def fullLog(fromRef: Option[String]) = proxy(FullLog, fromRef)
        def commitInfoLog() = proxy(CommitInfoLog)
        def tag(tag: String, tagMessage: String) = proxy(Tag, tag, tagMessage)
        def checkGitRepo() = proxy(CheckGitRepo)
      }
    }
}

object MainSpec extends DefaultRunnableSpec {
  def spec = suite("MainSpec")(
    suite("main") (
      testM("next should return") {
        val mockEnv: ULayer[Git with Console] = (
          GitMock.CheckGitRepo(unit) ++
            GitMock.CurrentBranch(value("main")) ++
            GitMock.CommitInfoLog(value("")) ++
            GitMock.FullLog(equalTo(None), value("")) ++
            MockConsole.PutStrLn(equalTo("0.1.0"))
          )
        val result = mainImpl(List("next")).provideCustomLayer(mockEnv)
        assertM(result)(isUnit)
      }
    )
  )

  // TODO stop this actually patching files!
  //  "patch" should "return " in {
  //    val result = new Main(fakeGit("main", "", "v0.0.0-1-gabcdef")).mainImpl(Array("patch"))
  //    result should be(Right(""))
  //  }
}
