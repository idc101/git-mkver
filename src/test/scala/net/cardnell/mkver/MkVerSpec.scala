package net.cardnell.mkver

import java.time.LocalDate

import net.cardnell.mkver.GitMock.{CheckGitRepo, CommitInfoLog}
import net.cardnell.mkver.MkVer._
import zio.ULayer
import zio.test.Assertion._
import zio.test._
import zio.test.mock.Expectation._

object MkVerSpec extends DefaultRunnableSpec {
  val log = """10be55f 10be55fc56c197f5e0159cfbfac22832b289182f  (HEAD -> zio)
              |f971636 f9716367b8692ed582206951d72bc7affc150f41  (test)
              |298326d 298326dd43677121724e9589f390cb0279cb8708  (tag: v0.2.0, tag: v0.3.0)
              |2e79c27 2e79c27e2faf85ea241e1911788fd3582c5176ce
              |699068c 699068cdec9193878cc1fcfc44c7dd6d004621ff  (tag: other)
              |320ed50 320ed50d79cbd585d6a28842a340d9742d9327b1
              |b3250df b3250df81f7ed389908a2aa89b32425a8ab8fb28  (tag: v0.1.0)
              |9ded7b1 9ded7b1edf3c066b8c15839304d0427b06cdd020
              |""".stripMargin

  def spec = suite("MkVerSpec")(
    suite("calcBumps")(
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
    ),
    suite("getCommitInfos")(
      testM("parse commit Info Log correctly") {
        val mockEnv: ULayer[Git] =
          CommitInfoLog returns value(log)
        val result = getCommitInfos("v").provideCustomLayer(mockEnv)
        assertM(result)(equalTo(List(
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
    ),
    suite("formatTag")(
      testM("should format tag") {
        val versionData = VersionData(1,2,3,4,"feature/f1", "abcd", "abcdefg", LocalDate.now())
        val branchConfig = BranchConfig(".*", "Version", true, "v", "release {Version}", "RC", List(Format("Version", "{Major}.{Minor}.{Patch}")), Nil)
        assertM(formatTag(branchConfig, versionData))(equalTo("v1.2.3"))
      }
    )
  )
}
