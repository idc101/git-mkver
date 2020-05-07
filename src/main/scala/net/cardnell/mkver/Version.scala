package net.cardnell.mkver

import java.time.LocalDate

case class Version(major: Int = 0,
                   minor: Int = 1,
                   patch: Int = 0,
                   prerelease: Option[String] = None,
                   buildmetadata: Option[String] = None) {
  def bump(bumps: VersionBumps): Version = {
    bumps match {
      case VersionBumps(true, _, _, _) => Version(this.major + 1, 0, 0)
      case VersionBumps(false, true, _, _) => Version(this.major, this.minor + 1, 0)
      case VersionBumps(false, false, true, _) => Version(this.major, this.minor, this.patch + 1)
      case _ => this
    }
  }
}

object Version {
  def parseTag(input: String, prefix: String): Option[Version] = {
    val version = ("^" + prefix + "(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-((?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\\+([0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?$").r

    input match {
      case version(major, minor, patch, prerelease, buildmetadata) =>
        Some(Version(major.toInt, minor.toInt, patch.toInt, Option(prerelease), Option(buildmetadata)))
      case _ =>
        None
    }
  }
}

case class VersionBumps(major: Boolean = false, minor: Boolean = false, patch: Boolean = false, commitCount: Int = 0) {
  def bumpMajor(): VersionBumps = this.copy(major = true)
  def bumpMinor(): VersionBumps = this.copy(minor = true)
  def bumpPatch(): VersionBumps = this.copy(patch = true)
  def bumpCommits(): VersionBumps = this.copy(commitCount = this.commitCount + 1)
  def noValidCommitMessages(): Boolean = { !major && !minor && !patch }
  }

object VersionBumps {
  val none = VersionBumps()
}

case class VersionData(major: Int,
                       minor: Int,
                       patch: Int,
                       commitCount: Int,
                       branch: String,
                       commitHashShort: String,
                       commitHashFull: String,
                       date: LocalDate)
