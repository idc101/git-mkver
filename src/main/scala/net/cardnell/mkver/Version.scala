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

case class Version(major: Int,
                   minor: Int,
                   patch: Int,
                   preRelease: Option[String] = None,
                   buildMetaData: Option[String] = None) {
  def getNextVersion(bumps: VersionBumps, bumpPreRelease: Boolean): NextVersion = {
    (preRelease, bumpPreRelease) match {
      case (Some(pr), false) =>
        // Generating a non-pre-release version after a pre-release is always the version without pre-release
        NextVersion(this.major, this.minor, this.patch, None)
      case (Some(pr), true) =>
        // Last version was a pre-release and next version is also a pre-release
        // Parse Last PreRelease Number
        val preReleaseNumber = "^.*(\\d+)$".r
        val nextPreReleaseNumber = pr match {
          case preReleaseNumber(lastPreReleaseNumber) => Some(lastPreReleaseNumber.toInt + 1)
          case _ => Some(1)
        }
        NextVersion(this.major, this.minor, this.patch, nextPreReleaseNumber)
      case (None, true) =>
        // Last version was not a pre-release but this version is
        // Bump the version number and add in the pre-release
        // ??? what if there is no bump - this would mean going from a released version to a pre-release
        // i.e. backwards!
        val nextPreReleaseNumber = Some(1)
        bump(bumps, nextPreReleaseNumber)
      case (None, false) =>
        // Not a pre-release previously and next version is not a pre-release either
        bump(bumps, None)
    }
  }

  def bump(bumps: VersionBumps, newPreRelease: Option[Int]): NextVersion = {
    bumps match {
      case VersionBumps(true, _, _, _) => NextVersion(this.major + 1, 0, 0, newPreRelease)
      case VersionBumps(false, true, _, _) => NextVersion(this.major, this.minor + 1, 0, newPreRelease)
      case VersionBumps(false, false, true, _) => NextVersion(this.major, this.minor, this.patch + 1, newPreRelease)
      case _ => NextVersion(this.major, this.minor, this.patch, newPreRelease)
    }
  }
}

object Version {
  def parseTag(input: String, prefix: String): Option[Version] = {
    val version = ("^" + prefix + "(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-((?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\\+([0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?$").r

    input match {
      case version(major, minor, patch, preRelease, buildMetaData) =>
        Some(Version(major.toInt, minor.toInt, patch.toInt, Option(preRelease), Option(buildMetaData)))
      case _ =>
        None
    }
  }
}

case class NextVersion(major: Int,
                       minor: Int,
                       patch: Int,
                       preReleaseNumber: Option[Int] = None)

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
                       preReleaseNumber: Option[Int],
                       commitCount: Int,
                       branch: String,
                       commitHashShort: String,
                       commitHashFull: String,
                       date: LocalDate)
