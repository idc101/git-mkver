package net.cardnell.mkver

import zio.test.Assertion.equalTo
import zio.test.{DefaultRunnableSpec, assert, suite, test}

object VersionSpec extends DefaultRunnableSpec {
  def spec = suite("VersionSpec")(
    suite("bump")(
      test("should bump correctly") {
        assert(Version(1,0,0).bump(VersionBumps(true, true, true, 0)))(equalTo(Version(2,0,0))) &&
          assert(Version(1,0,0).bump(VersionBumps(false, true, true, 0)))(equalTo(Version(1,1,0))) &&
          assert(Version(1,0,0).bump(VersionBumps(false, false, true, 0)))(equalTo(Version(1,0,1))) &&
          assert(Version(1,0,0).bump(VersionBumps(false, false, false, 0)))(equalTo(Version(1,0,0))) &&
          assert(Version(1,2,3).bump(VersionBumps(true, true, true, 0)))(equalTo(Version(2,0,0))) &&
          assert(Version(1,2,3).bump(VersionBumps(false, true, true, 0)))(equalTo(Version(1,3,0))) &&
          assert(Version(1,2,3).bump(VersionBumps(false, false, true, 0)))(equalTo(Version(1,2,4))) &&
          assert(Version(1,2,3).bump(VersionBumps(false, false, false, 0)))(equalTo(Version(1,2,3)))
      }
    ),
    suite("parseTag")(
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
  )
}
