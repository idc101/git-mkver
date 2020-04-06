package net.cardnell.mkver

import java.time.LocalDate

import zio.test.Assertion.equalTo
import zio.test.{assert, suite, test}

object FormatterSpec {

  val suite1 = suite("formatter")(
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
      val versionData = VersionData(1,2,3,4,"feature/f1", "abcd", "abcdefg", LocalDate.now(), "56")
      val branchConfig = BranchConfig(".*", "v", true, "Version", "release {Version}", "RC", Nil, Nil)
      val formatter = Formatter(versionData, branchConfig)
      assert(formatter.format("{x}"))(equalTo("1")) &&
        assert(formatter.format("{y}"))(equalTo("2")) &&
        assert(formatter.format("{z}"))(equalTo("3")) &&
        assert(formatter.format("{br}"))(equalTo("feature-f1"))
        //assert(formatter.format("{env.HOME}"))(equalTo("???")) - How to make this os agnostic?
    }
  )
}
