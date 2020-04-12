package net.cardnell.mkver

import net.cardnell.mkver.GitMock.{CheckGitRepo, CommitInfoLog, CurrentBranch}
import zio.test.Assertion.equalTo
import zio.test.mock.Expectation._
import zio.test.mock._
import zio.test.{assertM, suite, testM}
import zio.{Has, ULayer, URLayer, ZLayer}
import Main.mainImpl

// TODO >> @Mockable[Git.Service]
object GitMock {
  sealed trait Tag[I, A] extends Method[Git, I, A] {
    def envBuilder: URLayer[Has[Proxy], Git] =
      GitMock.envBuilder
  }

  object CurrentBranch extends Tag[Unit, String]
  object FullLog extends Tag[String, String]
  object CommitInfoLog extends Tag[Unit, String]
  object Tag extends Tag[(String, String), Unit]
  object CheckGitRepo extends Tag[Unit, Unit]

  private val envBuilder: URLayer[Has[Proxy], Git] =
    ZLayer.fromService(invoke =>
      new Git.Service {
        def currentBranch() = invoke(CurrentBranch)
        def fullLog(fromRef: String) = invoke(FullLog, fromRef)
        def commitInfoLog() = invoke(CommitInfoLog)
        def tag(tag: String, tagMessage: String) = invoke(Tag, tag, tagMessage)
        def checkGitRepo() = invoke(CheckGitRepo)
      }
    )
}

object MainSpec {
  val suite1 = suite("main") (
    testM("next should return") {
      val mockEnv: ULayer[Git] =
          (CheckGitRepo returns unit) andThen
          (CurrentBranch returns value("master")) andThen
          (CommitInfoLog returns value("v0.0.0-1-gabcdef"))
      val result = mainImpl(List("next")).provideCustomLayer(mockEnv)
      assertM(result)(equalTo("0.1.0"))
    },
    testM("tag should return") {
      val mockEnv: ULayer[Git] =
        (CheckGitRepo returns unit) andThen
          (CurrentBranch returns value("master")) andThen
          (CommitInfoLog returns value("v0.0.0-1-gabcdef"))
      val result = mainImpl(List("tag")).provideCustomLayer(mockEnv)
      assertM(result)(
        equalTo("")
      )
    }
  )

  // TODO stop this actually patching files!
//  "patch" should "return " in {
//    val result = new Main(fakeGit("master", "", "v0.0.0-1-gabcdef")).mainImpl(Array("patch"))
//    result should be(Right(""))
//  }
}
