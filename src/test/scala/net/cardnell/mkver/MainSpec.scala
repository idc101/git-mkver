package net.cardnell.mkver

import zio.{RIO, Task}
import zio.blocking.Blocking
import zio.test.Assertion.equalTo
import zio.test.{assertM, suite, testM}

object MainSpec {
  def fakeGit(currentBranchV: String = "", logV: String = "", describeV: String = "", tagV: String= "") = new Git.Service {
    override def currentBranch(): RIO[Blocking, String] = RIO.succeed(currentBranchV)
    override def fullLog(lastVersionTag: String): RIO[Blocking, String] = RIO.succeed(logV)
    override def commitInfoLog(): RIO[Blocking, String] = RIO.succeed(describeV)
    override def tag(tag: String, tagMessage: String): RIO[Blocking, Unit] = RIO.unit
    override def checkGitRepo(): RIO[Blocking, Unit] = RIO.unit
  }

  val suite1 = suite("main") (
    testM("next should return") {
      val result = new Main(fakeGit("master", "", "v0.0.0-1-gabcdef")).mainImpl(List("next"))
      assertM(result)(equalTo("0.1.0"))
    },
    testM("tag should return") {
      val result = new Main(fakeGit("master", "", "v0.0.0-1-gabcdef")).mainImpl(List("tag"))
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
