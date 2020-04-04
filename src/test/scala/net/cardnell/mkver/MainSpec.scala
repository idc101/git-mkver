package net.cardnell.mkver

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class MainSpec extends AnyFlatSpec with Matchers {
  def fakeGit(currentBranchV: String = "", logV: String = "", describeV: String = "", tagV: String= "") = new Git.Service {
    override def currentBranch(): String = currentBranchV
    override def log(lastVersionTag: String): String = logV
    override def describe(prefix: String): String = describeV
    override def tag(tag: String, tagMessage: String): Unit = tagV
    override def checkGitRepo(): Either[MkVerError, Unit] = Right(())
  }

  "next" should "return " in {
    val result = new Main(fakeGit("master", "", "v0.0.0-1-gabcdef")).mainImpl(Array("next"))
    result should be(Right("v0.1.0"))
  }

  "tag" should "return " in {
    val result = new Main(fakeGit("master", "", "v0.0.0-1-gabcdef")).mainImpl(Array("tag"))
    result should be(Right(""))
  }

  "patch" should "return " in {
    val result = new Main(fakeGit("master", "", "v0.0.0-1-gabcdef")).mainImpl(Array("patch"))
    result should be(Right(""))
  }
}
