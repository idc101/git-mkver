package net.cardnell.mkver

import zio.test.Assertion.equalTo
import zio.test.mock.Expectation._
import zio.test.mock._
import zio.test.{DefaultRunnableSpec, assertM, suite, testM}
import zio.{Has, ULayer, URLayer, ZLayer}
import Main.mainImpl

// TODO >> @Mockable[Git.Service]
object GitMock extends Mock[Git] {
//  sealed trait Tag[I, A] extends Method[Git, I, A] {
//    def envBuilder: URLayer[Has[Proxy], Git] =
//      GitMock.envBuilder
//  }

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
        val mockEnv: ULayer[Git] = (
          GitMock.CheckGitRepo(unit) ++
            GitMock.CurrentBranch(value("master")) ++
            GitMock.CommitInfoLog(value("")) ++
            GitMock.FullLog(equalTo(None), value(""))
          )
        val result = mainImpl(List("next")).provideCustomLayer(mockEnv)
        assertM(result)(equalTo("0.1.0"))
      },
      testM("tag should return") {
        val mockEnv: ULayer[Git] = (
          GitMock.CheckGitRepo(unit) ++
            GitMock.CurrentBranch(value("master")) ++
            GitMock.CommitInfoLog(value("")) ++
            GitMock.FullLog(equalTo(None), value(""))
          )
        val result = mainImpl(List("tag")).provideCustomLayer(mockEnv)
        assertM(result)(
          equalTo("")
        )
      }
    )
  )

  // TODO stop this actually patching files!
  //  "patch" should "return " in {
  //    val result = new Main(fakeGit("master", "", "v0.0.0-1-gabcdef")).mainImpl(Array("patch"))
  //    result should be(Right(""))
  //  }
}
