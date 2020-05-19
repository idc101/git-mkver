package net.cardnell.mkver

import zio.test._
import zio.test.Assertion._

object AppConfigSpec extends DefaultRunnableSpec {
  def spec = suite("AppConfigSpec") (
    suite("getBranchConfig") (
      testM("master should return master config") {
        assertM(AppConfig.getRunConfig(None, "master"))(
          hasField("name", _.name, equalTo("master"))
        )
      },
      testM("feat should return .* config") {
        assertM(AppConfig.getRunConfig(None, "feat"))(
          hasField("name", (c: RunConfig) => c.name, equalTo(".*")) &&
            hasField("formats", _.formats, contains(Format("BuildMetaData", "{Branch}.{ShortHash}")))
        )
      }
    ),
    suite("mergeFormat")(
      test("should merge formats") {
        val f1 = Format("f1", "v1")
        val f2 = Format("f2", "v2")
        val f3 = Format("f3", "v3")
        val f1b = Format("f1", "v4")
        assert(AppConfig.mergeFormats(List(f1, f3), List(f1b, f2)))(equalTo(List(f1b, f2, f3).sortBy(_.name)))
      }
    ),
    suite("mergeFormat")(
      testM("should merge formats on real config") {
        assertM(AppConfig.getRunConfig(None, "master").map(_.formats))(equalTo(List(Format("BuildMetaData","{Branch}.{ShortHash}"))))
      }
    )
  )
}
