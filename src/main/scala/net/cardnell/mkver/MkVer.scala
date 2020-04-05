package net.cardnell.mkver

import java.time.LocalDate

case class VersionData(major: Int,
                       minor: Int,
                       patch: Int,
                       commitCount: Int,
                       branch: String,
                       commitHashShort: String,
                       commitHashFull: String,
                       date: LocalDate,
                       buildNo: String)

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

case class DescribeInfo(lastTag: String, commitCount: Int, commitHash: String)

case class VersionBumps(major: Boolean = false, minor: Boolean = false, patch: Boolean = false, commitCount: Int = 0) {
  def bumpMajor(): VersionBumps = this.copy(major = true)
  def bumpMinor(): VersionBumps = this.copy(minor = true)
  def bumpPatch(): VersionBumps = this.copy(patch = true)
  def bumpCommits(): VersionBumps = this.copy(commitCount = this.commitCount + 1)
}

object VersionBumps {
  val minVersionBump = VersionBumps(major = false, minor = true, patch = false)
}

object MkVer {
  def getDescribeInfo(git: Git.Service, prefix: String): DescribeInfo = {
    val describeResult = git.describe(prefix)

    // what if it was a branch tag that contains "-"?
    val describe = "^(.*)-(\\d+)-g([0-9a-f]{5,40})$".r

    describeResult match {
      case describe(tag, commitCount, hash) => DescribeInfo(tag, commitCount.toInt, hash)
    }
  }

  def getLastVersion(prefix: String, lastVersionTag: String): Either[MkVerError, Version] = {
    val version = ("^" + prefix + "(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-((?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\\+([0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?$").r

    lastVersionTag match {
      case version(major, minor, patch, prerelease, buildmetadata) =>
        Right(Version(major.toInt, minor.toInt, patch.toInt, Option(prerelease), Option(buildmetadata)))
      case _ =>
        Left(MkVerError(s"fatal: unable to parse last tag. ($lastVersionTag) doesn't match a SemVer pattern"))
    }
  }

  def formatTag(config: BranchConfig, versionData: VersionData): Either[MkVerError, String] = {
    val allowedFormats = Formatter.builtInFormats.map(_.name)
    if (!allowedFormats.contains(config.tagFormat)) {
      Left(MkVerError(s"tagFormat (${config.tagFormat}) must be one of: ${allowedFormats.mkString(", ")}"))
    } else {
      Right(Formatter(versionData, config).format(s"${config.prefix}{${config.tagFormat}}"))
    }
  }

  def getNextVersion(git: Git.Service, config: BranchConfig, currentBranch: String): Either[MkVerError, VersionData] = {
    val describeInfo = getDescribeInfo(git, config.prefix)
    getLastVersion(config.prefix, describeInfo.lastTag).map { lastVersion =>
      // If no commits since last tag then there is no next version yet - make bumps = 0
      val bumps = if (describeInfo.commitCount == 0) VersionBumps() else getVersionBumps(git, describeInfo.lastTag)
      val nextVersion = lastVersion.bump(bumps)
      VersionData(
        major = nextVersion.major,
        minor = nextVersion.minor,
        patch = nextVersion.patch,
        commitCount = describeInfo.commitCount,
        branch = currentBranch,
        commitHashShort = describeInfo.commitHash,
        commitHashFull = "TODO",
        date = LocalDate.now(),
        buildNo = sys.env.getOrElse("BUILD_BUILDNUMBER", "TODO")
      )
    }
  }

  def getVersionBumps(git: Git.Service, lastVersionTag: String): VersionBumps = {
    val log = git.log(lastVersionTag)

    val logBumps: VersionBumps = calcBumps(log.linesIterator.toList, VersionBumps())
    if (logBumps == VersionBumps()) {
      VersionBumps.minVersionBump
    } else {
      logBumps
    }
  }

  def calcBumps(lines: List[String], bumps: VersionBumps): VersionBumps = {
    val breaking = "BREAKING CHANGE".r
    val major = "major(\\(.+\\))?:".r
    val minor = "minor(\\(.+\\))?:".r
    val patch = "patch(\\(.+\\))?:".r
    val feat = "feat(\\(.+\\))?:".r
    val fix = "fix(\\(.+\\))?:".r
    if (lines.isEmpty) {
      bumps
    } else {
      val line = lines.head
      if (line.startsWith("commit")) {
        calcBumps(lines.tail, bumps.bumpPatch())
      } else if (line.startsWith("    ")) {
        if (major.findFirstIn(line).nonEmpty || breaking.findFirstIn(line).nonEmpty) {
          calcBumps(lines.tail, bumps.bumpMajor())
        } else if (minor.findFirstIn(line).nonEmpty || feat.findFirstIn(line).nonEmpty) {
          calcBumps(lines.tail, bumps.bumpMinor())
        } else if (patch.findFirstIn(line).nonEmpty || fix.findFirstIn(line).nonEmpty) {
          calcBumps(lines.tail, bumps.bumpPatch())
        } else {
          calcBumps(lines.tail, bumps)
        }
      } else {
        calcBumps(lines.tail, bumps)
      }
    }
  }
}
