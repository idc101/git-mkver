package net.cardnell.mkver

import java.time.LocalDate

import MkVer._
import zio.{RIO, Task}
import zio.blocking.Blocking
import zio.test.Assertion._
import zio.test._

object VersionSpec {
  val suite1 = suite("Version")(
    test("should bump correctly") {
      assert(Version(1,0,0).bump(VersionBumps(true, true, true, 0)))(equalTo(Version(2,0,0))) &&
      assert(Version(1,0,0).bump(VersionBumps(false, true, true, 0)))(equalTo(Version(1,1,0))) &&
      assert(Version(1,0,0).bump(VersionBumps(false, false, true, 0)))(equalTo(Version(1,0,1))) &&
      assert(Version(1,0,0).bump(VersionBumps(false, false, false, 0)))(equalTo(Version(1,0,0))) &&
      assert(Version(1,2,3).bump(VersionBumps(true, true, true, 0)))(equalTo(Version(2,0,0))) &&
      assert(Version(1,2,3).bump(VersionBumps(false, true, true, 0)))(equalTo(Version(1,3,0))) &&
      assert(Version(1,2,3).bump(VersionBumps(false, false, true, 0)))(equalTo(Version(1,2,4))) &&
      assert(Version(1,2,3).bump(VersionBumps(false, false, false, 0)))(equalTo(Version(1,2,3)))
    },
    test("should parse Version correctly") {
      assert(Version.parseTag("v10.5.3", "v"))(equalTo(Some(Version(10, 5, 3, None, None))))
    },
    test("should parse VersionPreRelease correctly") {
      assert(Version.parseTag("v10.5.3-pre", "v"))(equalTo(Some(Version(10, 5, 3, Some("pre"), None))))
    },
    test("should parse VersionPreReleaseBuildMetaData correctly") {
      assert(Version.parseTag("v10.5.3-pre+build", "v"))(equalTo(Some(Version(10, 5, 3, Some("pre"), Some("build")))))
    },
    test("should parse VersionBuildMetaData correctly") {
      assert(Version.parseTag("v10.5.3+build", "v"))(equalTo(Some(Version(10, 5, 3, None, Some("build")))))
    },
    test("should parse VersionPreReleaseBuildMetaData with hyphen in PreRelease correctly") {
      assert(Version.parseTag("v10.5.3-pre-3+build", "v"))(equalTo(Some(Version(10, 5, 3, Some("pre-3"), Some("build")))))
    },
    test("should parse VersionPreReleaseBuildMetaData with hyphen in PreRelease and BuildMetaData correctly") {
      assert(Version.parseTag("v10.5.3-pre-3+build-1", "v"))(equalTo(Some(Version(10, 5, 3, Some("pre-3"), Some("build-1")))))
    },
    test("should parse VersionBuildMetaData with hyphen in BuildMetaData correctly") {
      assert(Version.parseTag("v10.5.3+build-1", "v"))(equalTo(Some(Version(10, 5, 3, None, Some("build-1")))))
    }
  )
}

object MkVerSpec {
  val log = """10be55f 10be55fc56c197f5e0159cfbfac22832b289182f  (HEAD -> zio)
              |f971636 f9716367b8692ed582206951d72bc7affc150f41  (test)
              |298326d 298326dd43677121724e9589f390cb0279cb8708  (tag: v0.2.0, tag: v0.3.0)
              |2e79c27 2e79c27e2faf85ea241e1911788fd3582c5176ce
              |699068c 699068cdec9193878cc1fcfc44c7dd6d004621ff  (tag: other)
              |320ed50 320ed50d79cbd585d6a28842a340d9742d9327b1
              |b3250df b3250df81f7ed389908a2aa89b32425a8ab8fb28  (tag: v0.1.0)
              |9ded7b1 9ded7b1edf3c066b8c15839304d0427b06cdd020
              |""".stripMargin
  def fakeGit(currentBranchV: String = "", logV: String = "", commitInfoLogV: String = log, tagV: String= "") = new Git.Service {
    override def currentBranch(): RIO[Blocking, String] = RIO.succeed(currentBranchV)
    override def fullLog(lastVersionTag: String): RIO[Blocking, String] = RIO.succeed(logV)
    override def commitInfoLog(): RIO[Blocking, String] = RIO.succeed(commitInfoLogV)
    override def tag(tag: String, tagMessage: String): RIO[Blocking, Unit] = RIO.unit
    override def checkGitRepo(): RIO[Blocking, Unit] = RIO.unit
  }

  val suite1 = suite("calcBumps")(
    test("should parse correctly") {
      assert(calcBumps(List("    major: change"), VersionBumps()))(equalTo(VersionBumps(major = true))) &&
      assert(calcBumps(List("    feat: change", "    BREAKING CHANGE"), VersionBumps()))(equalTo(VersionBumps(major = true, minor = true))) &&
      assert(calcBumps(List("    feat: change"), VersionBumps()))(equalTo(VersionBumps(minor = true))) &&
      assert(calcBumps(List("    fix: change"), VersionBumps()))(equalTo(VersionBumps(patch = true))) &&
      assert(calcBumps(List("    patch: change"), VersionBumps()))(equalTo(VersionBumps(patch = true))) &&
      assert(calcBumps(List("    major(a component): change"), VersionBumps()))(equalTo(VersionBumps(major = true))) &&
      assert(calcBumps(List("    feat(a component): change", "    BREAKING CHANGE"), VersionBumps()))(equalTo(VersionBumps(major = true, minor = true))) &&
      assert(calcBumps(List("    feat(a component): change"), VersionBumps()))(equalTo(VersionBumps(minor = true))) &&
      assert(calcBumps(List("    fix(a component): change"), VersionBumps()))(equalTo(VersionBumps(patch = true))) &&
      assert(calcBumps(List("    patch(a component): change"), VersionBumps()))(equalTo(VersionBumps(patch = true))) &&
      assert(calcBumps(List("    some random commit"), VersionBumps()))(equalTo(VersionBumps()))
    }
  )

  val suite2 = suite("getCommitInfos")(
    testM("parse commit Info Log correctly") {
      assertM(getCommitInfos(fakeGit(), "v"))(equalTo(List(
        CommitInfo("10be55f", "10be55fc56c197f5e0159cfbfac22832b289182f", 0, List()),
        CommitInfo("f971636", "f9716367b8692ed582206951d72bc7affc150f41", 1, List()),
        CommitInfo("298326d", "298326dd43677121724e9589f390cb0279cb8708", 2, List(Version(0, 2, 0), Version(0, 3, 0))),
        CommitInfo("2e79c27", "2e79c27e2faf85ea241e1911788fd3582c5176ce", 3, List()),
        CommitInfo("699068c", "699068cdec9193878cc1fcfc44c7dd6d004621ff", 4, List()),
        CommitInfo("320ed50", "320ed50d79cbd585d6a28842a340d9742d9327b1", 5, List()),
        CommitInfo("b3250df", "b3250df81f7ed389908a2aa89b32425a8ab8fb28", 6, List(Version(0, 1, 0))),
        CommitInfo("9ded7b1", "9ded7b1edf3c066b8c15839304d0427b06cdd020", 7, List()),
      )))
    }
  )

  val suite3 = suite("formatTag")(
    testM("should format tag") {
      val versionData = VersionData(1,2,3,4,"feature/f1", "abcd", "abcdefg", LocalDate.now(), "56")
      val branchConfig = BranchConfig(".*", "v", true, "Version", "release {Version}", "RC", List(Format("Version", "{x}.{y}.{z}")), Nil)
      assertM(formatTag(branchConfig, versionData))(equalTo("v1.2.3"))
    }
  )

//  "getNextVersion" should "return next version data" in {
//    val versionData = VersionData(1,2,3,4,"feature/f1", "abcd", "abcdefg", LocalDate.now(), "56")
//    val branchConfig = BranchConfig(".*", "v", true, "Version", "release {Version}", "RC", List(Format("Version", "{x}.{y}.{z}")), Nil)
//    formatTag(branchConfig, versionData) should be(Right("v1.2.3"))
//  }
}
