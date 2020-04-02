package net.cardnell.mkver

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import MkVer._

class VersionSpec extends AnyFlatSpec with Matchers {
  "bump" should "bump correctly" in {
    Version(1,0,0).bump(VersionBumps(true, true, true, 0)) should be(Version(2,0,0))
    Version(1,0,0).bump(VersionBumps(false, true, true, 0)) should be(Version(1,1,0))
    Version(1,0,0).bump(VersionBumps(false, false, true, 0)) should be(Version(1,0,1))
    Version(1,0,0).bump(VersionBumps(false, false, false, 0)) should be(Version(1,0,0))
  }
}

class MkVerSpec extends AnyFlatSpec with Matchers {
  def fakeGit(currentBranchV: String = "", logV: String = "", describeV: String = "", tagV: String= "") = new Git.Service {
    override def currentBranch(): String = currentBranchV
    override def log(lastVersionTag: String): String = logV
    override def describe(prefix: String): String = describeV
    override def tag(tag: String, tagMessage: String): Unit = tagV
    override def checkGitRepo(): Unit = ()
  }

  "getDescribeInfo" should "parse correctly" in {
    getDescribeInfo(fakeGit(describeV = "v1.0.0-5-gabcde"), "v") should be(DescribeInfo("v1.0.0", 5, "abcde"))
    getDescribeInfo(fakeGit(describeV = "v1.0.0-pre-5-gabcde"), "v") should be(DescribeInfo("v1.0.0-pre", 5, "abcde"))
    getDescribeInfo(fakeGit(describeV = "v1.0.0+build-5-gabcde"), "v") should be(DescribeInfo("v1.0.0+build", 5, "abcde"))
    getDescribeInfo(fakeGit(describeV = "v1.0.0-pre+build-5-gabcde"), "v") should be(DescribeInfo("v1.0.0-pre+build", 5, "abcde"))
  }

  "calcBumps" should "parse correctly" in {
    calcBumps(List("    major: change"), VersionBumps()) should be(VersionBumps(major = true))
    calcBumps(List("    feat: change", "    BREAKING CHANGE"), VersionBumps()) should be(VersionBumps(major = true, minor = true))
    calcBumps(List("    feat: change"), VersionBumps()) should be(VersionBumps(minor = true))
    calcBumps(List("    fix: change"), VersionBumps()) should be(VersionBumps(patch = true))
    calcBumps(List("    patch: change"), VersionBumps()) should be(VersionBumps(patch = true))
    calcBumps(List("    major(a component): change"), VersionBumps()) should be(VersionBumps(major = true))
    calcBumps(List("    feat(a component): change", "    BREAKING CHANGE"), VersionBumps()) should be(VersionBumps(major = true, minor = true))
    calcBumps(List("    feat(a component): change"), VersionBumps()) should be(VersionBumps(minor = true))
    calcBumps(List("    fix(a component): change"), VersionBumps()) should be(VersionBumps(patch = true))
    calcBumps(List("    patch(a component): change"), VersionBumps()) should be(VersionBumps(patch = true))
    calcBumps(List("    some random commit"), VersionBumps()) should be(VersionBumps())
  }

  "getLastVersion" should "parse correctly" in {
    getLastVersion("v", "v10.5.3") should be(Version(10, 5, 3, None, None))
    getLastVersion("v", "v10.5.3-pre") should be(Version(10, 5, 3, Some("pre"), None))
    getLastVersion("v", "v10.5.3-pre+build") should be(Version(10, 5, 3, Some("pre"), Some("build")))
    getLastVersion("v", "v10.5.3+build") should be(Version(10, 5, 3, None, Some("build")))
    getLastVersion("v", "v10.5.3-pre-3+build") should be(Version(10, 5, 3, Some("pre-3"), Some("build")))
    //getLastVersion("v", "v10.5.3-pre-3+build+1") should be(Version(10, 5, 3, Some("pre-3"), Some("build+1")))
    getLastVersion("v", "v10.5.3-pre-3+build-1") should be(Version(10, 5, 3, Some("pre-3"), Some("build-1")))
    getLastVersion("v", "v10.5.3+build-1") should be(Version(10, 5, 3, None, Some("build-1")))
  }
}
