package net.cardnell.mkver

import java.time.LocalDate

sealed trait VersionMode

object VersionMode {
  case object SemVer extends VersionMode
  case object SemVerPreRelease extends VersionMode
  case object YearMonth extends VersionMode
  case object YearMonthPreRelease extends VersionMode

  def read(value: String): Either[String, VersionMode] =
    value match {
      case "SemVer" => Right(SemVer)
      case _ => Left("VersionMode must be one of: ")
    }
}

sealed trait IncrementAction

object IncrementAction {
  case object Fail extends IncrementAction
  case object IncrementMajor extends IncrementAction
  case object IncrementMinor extends IncrementAction
  case object IncrementPatch extends IncrementAction
  case object NoIncrement extends IncrementAction

  def read(value: String): Either[String, IncrementAction] =
    value match {
      case "Fail" => Right(Fail)
      case "IncrementMajor" => Right(IncrementMajor)
      case "IncrementMinor" => Right(IncrementMinor)
      case "IncrementPatch" => Right(IncrementPatch)
      case "NoIncrement" => Right(NoIncrement)
      case _ => Left("IncrementAction should be one of Fail|IncrementMajor|IncrementMinor|IncrementPatch|NoIncrement")
    }
}

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
  def bump(incrementAction: IncrementAction): VersionBumps = {
    incrementAction match {
      case IncrementAction.IncrementMajor => this.copy(major = true)
      case IncrementAction.IncrementMinor => this.copy(minor = true)
      case IncrementAction.IncrementPatch => this.copy(patch = true)
      case _ => this
    }
  }
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
