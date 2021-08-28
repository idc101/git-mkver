package net.cardnell.mkver

import zio.test.Assertion.equalTo
import zio.test.{DefaultRunnableSpec, assert, suite, test}

object VersionSpec extends DefaultRunnableSpec {
  def spec = suite("VersionSpec")(
    suite("getNextVersion")(
      test("should return correctly") {
        val vb = VersionBumps(true, true, true, 0)
        assert(Version(1,2,3,Some("RC4")).getNextVersion(vb, false))(equalTo(NextVersion(1,2,3,None))) &&
          assert(Version(1,2,3,Some("RC4")).getNextVersion(vb, true))(equalTo(NextVersion(1,2,3,Some(5)))) &&
          assert(Version(1,2,3,Some("RC")).getNextVersion(vb, true))(equalTo(NextVersion(1,2,3,Some(1)))) &&
          assert(Version(1,2,3,None).getNextVersion(vb, true))(equalTo(NextVersion(2,0,0,Some(1)))) &&
          assert(Version(1,2,3,None).getNextVersion(vb, false))(equalTo(NextVersion(2,0,0,None)))
      },
      test("should return correctly with commit override") {
        val vb = VersionBumps(true, true, true, 0, None, Some(Version(3,4,5)))
        assert(Version(1,2,3,Some("RC4")).getNextVersion(vb, false))(equalTo(NextVersion(3,4,5,None))) &&
          assert(Version(1,2,3,Some("RC4")).getNextVersion(vb, true))(equalTo(NextVersion(3,4,5,Some(1)))) &&
          assert(Version(1,2,3,Some("RC")).getNextVersion(vb, true))(equalTo(NextVersion(3,4,5,Some(1)))) &&
          assert(Version(1,2,3,None).getNextVersion(vb, true))(equalTo(NextVersion(3,4,5,Some(1)))) &&
          assert(Version(1,2,3,None).getNextVersion(vb, false))(equalTo(NextVersion(3,4,5,None)))
      },
      test("should return correctly with branch override") {
        val vb = VersionBumps(true, true, true, 0, Some(Version(3,4,5)), None)
        assert(Version(1,2,3,Some("RC4")).getNextVersion(vb, false))(equalTo(NextVersion(3,4,5,None))) &&
          assert(Version(1,2,3,Some("RC4")).getNextVersion(vb, true))(equalTo(NextVersion(3,4,5,Some(1)))) &&
          assert(Version(1,2,3,Some("RC")).getNextVersion(vb, true))(equalTo(NextVersion(3,4,5,Some(1)))) &&
          assert(Version(1,2,3,None).getNextVersion(vb, true))(equalTo(NextVersion(3,4,5,Some(1)))) &&
          assert(Version(1,2,3,None).getNextVersion(vb, false))(equalTo(NextVersion(3,4,5,None)))
      }
    ),
    suite("bump")(
      test("should bump correctly") {
        assert(Version(1,0,0).bump(VersionBumps(true, true, true, 0, Some(Version(3,0,0)), None), Some(6)))(equalTo(NextVersion(3,0,0,Some(6)))) &&
          assert(Version(1,0,0).bump(VersionBumps(true, true, true, 0, None, Some(Version(4,0,0))), Some(6)))(equalTo(NextVersion(4,0,0,Some(6)))) &&
          assert(Version(1,0,0).bump(VersionBumps(true, true, true, 0, Some(Version(3,0,0)), Some(Version(4,0,0))), Some(6)))(equalTo(NextVersion(4,0,0,Some(6)))) &&
          assert(Version(1,0,0).bump(VersionBumps(true, true, true, 0), Some(6)))(equalTo(NextVersion(2,0,0,Some(6)))) &&
          assert(Version(1,0,0).bump(VersionBumps(false, true, true, 0), Some(6)))(equalTo(NextVersion(1,1,0,Some(6)))) &&
          assert(Version(1,0,0).bump(VersionBumps(false, false, true, 0), Some(6)))(equalTo(NextVersion(1,0,1,Some(6)))) &&
          assert(Version(1,0,0).bump(VersionBumps(false, false, false, 0), Some(6)))(equalTo(NextVersion(1,0,0,Some(6)))) &&
          assert(Version(1,2,3).bump(VersionBumps(true, true, true, 0), Some(6)))(equalTo(NextVersion(2,0,0,Some(6)))) &&
          assert(Version(1,2,3).bump(VersionBumps(false, true, true, 0), Some(6)))(equalTo(NextVersion(1,3,0,Some(6)))) &&
          assert(Version(1,2,3).bump(VersionBumps(false, false, true, 0), Some(6)))(equalTo(NextVersion(1,2,4,Some(6)))) &&
          assert(Version(1,2,3).bump(VersionBumps(false, false, false, 0), Some(6)))(equalTo(NextVersion(1,2,3,Some(6))))
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
    ),
    suite("ordering")(
      test("less than") {
        assert(Version.versionOrdering.lt(Version(0, 2, 3, None, None), Version(1, 2, 3, None, None)))(equalTo(true))
        assert(Version.versionOrdering.lt(Version(1, 1, 3, None, None), Version(1, 2, 3, None, None)))(equalTo(true))
        assert(Version.versionOrdering.lt(Version(1, 2, 2, None, None), Version(1, 2, 3, None, None)))(equalTo(true))
      },
      test("equal") {
        assert(Version.versionOrdering.lt(Version(1, 2, 3, None, None), Version(1, 2, 3, None, None)))(equalTo(false))
      },
      test("greater than") {
        assert(Version.versionOrdering.lt(Version(2, 2, 3, None, None), Version(1, 2, 3, None, None)))(equalTo(false))
        assert(Version.versionOrdering.lt(Version(1, 3, 3, None, None), Version(1, 2, 3, None, None)))(equalTo(false))
        assert(Version.versionOrdering.lt(Version(1, 2, 4, None, None), Version(1, 2, 3, None, None)))(equalTo(false))
      },
      test("pre-release vs non pre-release less than") {
        assert(Version.versionOrdering.lt(Version(1, 2, 3, Some("RC1"), None), Version(1, 2, 3, None, None)))(equalTo(true))
        assert(Version.versionOrdering.lt(Version(1, 2, 3, None, None), Version(1, 2, 3, Some("RC1"), None)))(equalTo(false))
      },
      test("pre-release vs pre-release less than") {
        assert(Version.versionOrdering.lt(Version(1, 2, 3, Some("RC1"), None), Version(1, 2, 3, Some("RC2"), None)))(equalTo(true))
        assert(Version.versionOrdering.lt(Version(1, 2, 3, Some("RC2"), None), Version(1, 2, 3, Some("RC1"), None)))(equalTo(false))
      },
    )
  )
}
