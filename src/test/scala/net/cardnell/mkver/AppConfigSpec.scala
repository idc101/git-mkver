package net.cardnell.mkver

import zio.test._
import zio.test.Assertion._

object AllSuites extends DefaultRunnableSpec {

  def spec = suite("All tests")(
    AppConfigSpec.suite1, AppConfigSpec.suite2, AppConfigSpec.suite3,
    FormatterSpec.suite1,
    VersionSpec.suite1,
    MkVerSpec.suite1,
    MkVerSpec.suite2,
    MkVerSpec.suite3,
    MainSpec.suite1,
    EndToEndTests.suite1
  )
}

object AppConfigSpec {
  val suite1 = suite("getBranchConfig") (
    testM("master should return master config") {
      assertM(AppConfig.getBranchConfig(None, "master"))(
        hasField("name", _.name, equalTo("master"))
      )
    },
    testM("feat should return .* config") {
      assertM(AppConfig.getBranchConfig(None, "feat"))(
        hasField("name", (c:BranchConfig) => c.name, equalTo(".*")) &&
        hasField("formats", _.formats, contains(Format("BuildMetaData", "{br}.{sh}")))
      )
    }
  )

  val suite2 = suite("mergeFormat")(
    test("should merge formats") {
      val f1 = Format("f1", "v1")
      val f2 = Format("f2", "v2")
      val f3 = Format("f3", "v3")
      val f1b = Format("f1", "v4")
      assert(AppConfig.mergeFormats(List(f1, f3), List(f1b, f2)))(equalTo(List(f1b, f2, f3).sortBy(_.name)))
    }
  )

  val suite3 = suite("mergeFormat")(
    testM("should merge formats") {
      val branchConfig = BranchConfig(".*", "v", true, "Version", "release {Version}", "RC", Nil, Nil)
      assertM(AppConfig.getPatchConfigs(None, branchConfig))(equalTo(Nil))
    }
  )
}
