package net.cardnell.mkver

import java.time.LocalDate

import zio.test.Assertion.equalTo
import zio.test.{DefaultRunnableSpec, assert, suite, test}

object FormatterSpec extends DefaultRunnableSpec {

  def spec = suite("FormatterSpec")(
    test("format should replace variables") {
      val formatter = Formatter.Formatter(List(
        Format("a", "1"),
        Format("a3", "3"),
        Format("b", "2"),
        Format("c", "{a}.{b}"),
        Format("r1", "{r2}")
      ))
      assert(formatter.format("hello"))(equalTo("hello")) &&
      assert(formatter.format("{a}"))(equalTo("1")) &&
      assert(formatter.format("{a}-{c}"))(equalTo("1-1.2")) &&
      assert(formatter.format("{r1}"))(equalTo("{r2}")) &&
      assert(formatter.format("{a3}"))(equalTo("3"))
    },
    test("branchNameToVariable should sanitize name") {
      assert(Formatter.branchNameToVariable("refs/heads/feat/f1"))(equalTo("feat-f1"))
    },
    test("should format default variables") {
      val versionData = VersionData(1,2,3, None, 4, "feature/f1", "abcd", "abcdefg", LocalDate.now())
      val runConfig = RunConfig(true, "v", "release {Version}", "RC", "{Branch}", false, Nil, IncrementAction.IncrementMinor, Nil, Nil)
      val formatter = Formatter(versionData, runConfig, false)
      assert(formatter.format("{Major}"))(equalTo("1")) &&
        assert(formatter.format("{Minor}"))(equalTo("2")) &&
        assert(formatter.format("{Patch}"))(equalTo("3")) &&
        assert(formatter.format("{Branch}"))(equalTo("feature-f1"))
        //assert(formatter.format("{env.HOME}"))(equalTo("???")) - How to make this os agnostic?
    },
    test("Next pre-release=false buildmetadata=false") {
      val versionData = VersionData(1,2,3, Some(3), 4, "feature/f1", "abcd", "abcdefg", LocalDate.now())
      val runConfig = RunConfig(true, "v", "release {Version}", "RC", "{Branch}", false, Nil, IncrementAction.IncrementMinor, Nil, Nil)
      val formatter = Formatter(versionData, runConfig, false)
      assert(formatter.format("{Next}"))(equalTo("1.2.3"))
    },
    test("Next pre-release=false buildmetadata=true") {
      val versionData = VersionData(1,2,3, Some(3), 4, "feature/f1", "abcd", "abcdefg", LocalDate.now())
      val runConfig = RunConfig(true, "v", "release {Version}", "RC", "{Branch}", true, Nil, IncrementAction.IncrementMinor, Nil, Nil)
      val formatter = Formatter(versionData, runConfig, false)
      assert(formatter.format("{Next}"))(equalTo("1.2.3+feature-f1"))
    },
    test("Next pre-release=true buildmetadata=false") {
      val versionData = VersionData(1,2,3, Some(3), 4, "feature/f1", "abcd", "abcdefg", LocalDate.now())
      val runConfig = RunConfig(true, "v", "release {Version}", "RC{PreReleaseNumber}", "{Branch}", false, Nil, IncrementAction.IncrementMinor, Nil, Nil)
      val formatter = Formatter(versionData, runConfig, true)
      assert(formatter.format("{Next}"))(equalTo("1.2.3-RC3"))
    },
    test("Next pre-release=true buildmetadata=true") {
      val versionData = VersionData(1,2,3, Some(3), 4, "feature/f1", "abcd", "abcdefg", LocalDate.now())
      val runConfig = RunConfig(true, "v", "release {Version}", "RC{PreReleaseNumber}", "{Branch}", true, Nil, IncrementAction.IncrementMinor, Nil, Nil)
      val formatter = Formatter(versionData, runConfig, true)
      assert(formatter.format("{Next}"))(equalTo("1.2.3-RC3+feature-f1"))
    },
    test("Picks up custom formats") {
      val versionData = VersionData(1,2,3, None, 0, "main", "abcd", "abcdefg", LocalDate.now())
      val formats = List(Format("Docker", "docker - {Version}"))
      val runConfig = RunConfig(true, "v", "release {Version}", "RC{PreReleaseNumber}", "{Branch}", true, Nil, IncrementAction.IncrementMinor, formats, Nil)
      val formatter = Formatter(versionData, runConfig, false)
      assert(formatter.format("{Docker}"))(equalTo("docker - 1.2.3"))
    }
  )
}
