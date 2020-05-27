package net.cardnell.mkver

import zio.test._
import zio.test.Assertion._

object AppConfigSpec extends DefaultRunnableSpec {
  def spec = suite("AppConfigSpec") (
    suite("getRunConfig") (
      testM("master should return master config") {
        assertM(AppConfig.getRunConfig(None, "master"))(
          hasField("tag", _.tag, equalTo(true))
        )
      },
      testM("feat should return .* config") {
        assertM(AppConfig.getRunConfig(None, "feat"))(
          hasField("tag", (c: RunConfig) => c.tag, equalTo(false)) &&
            hasField("buildMetaDataFormat", (c: RunConfig) => c.buildMetaDataFormat, equalTo("{Branch}.{ShortHash}"))
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
    )
  )
}
