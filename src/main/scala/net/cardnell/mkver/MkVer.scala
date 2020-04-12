package net.cardnell.mkver

import java.time.LocalDate

import zio.blocking.Blocking
import zio.{IO, RIO, Task, ZIO}

case class CommitInfo(shortHash: String, fullHash: String, commitsBeforeHead: Int, tags: List[Version])

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

case class LastVersion(commitHash: String, commitsBeforeHead: Int, version: Version)

case class DescribeInfo(lastTag: String, commitCount: Int, commitHash: String)

case class VersionBumps(major: Boolean = false, minor: Boolean = false, patch: Boolean = false, commitCount: Int = 0) {
  def bumpMajor(): VersionBumps = this.copy(major = true)
  def bumpMinor(): VersionBumps = this.copy(minor = true)
  def bumpPatch(): VersionBumps = this.copy(patch = true)
  def bumpCommits(): VersionBumps = this.copy(commitCount = this.commitCount + 1)
}

object VersionBumps {
  val minVersionBump = VersionBumps(major = false, minor = true, patch = false)
  val none = VersionBumps()
}

object MkVer {
  def getCommitInfos(prefix: String): RIO[Git with Blocking, List[CommitInfo]]= {
    val lineMatch = "^([0-9a-f]{5,40}) ([0-9a-f]{5,40}) *(\\((.*)\\))?$".r

    Git.commitInfoLog().map { log =>
      log.lines.zipWithIndex.flatMap {
        case (line, i) => {
          line match {
            case lineMatch(shortHash, longHash, _, names) => {
              val versions = Option(names).getOrElse("").split(",").toList
                .map(_.trim)
                .filter(_.startsWith("tag: "))
                .map(_.replace("tag: ", ""))
                .flatMap(Version.parseTag(_, prefix))
              Some(CommitInfo(shortHash, longHash, i, versions))
            }
            case _ => None
          }
        }
      }.toList
    }
  }

  def getLastVersion(commitInfos: List[CommitInfo]): Option[LastVersion] = {
    commitInfos.find(_.tags.nonEmpty).map(ci => LastVersion(ci.fullHash, ci.commitsBeforeHead, ci.tags.head))
  }

  def formatTag(config: BranchConfig, versionData: VersionData, formatAsTag: Boolean = true): Task[String] = {
    val allowedFormats = Formatter.builtInFormats.map(_.name)
    if (!allowedFormats.contains(config.tagFormat)) {
      IO.fail(MkVerException(s"tagFormat (${config.tagFormat}) must be one of: ${allowedFormats.mkString(", ")}"))
    } else {
      if (formatAsTag) {
        Task.effect(Formatter(versionData, config).format("{Tag}"))
      } else {
        Task.effect(Formatter(versionData, config).format("{Next}"))
      }
    }
  }

  def getNextVersion(config: BranchConfig, currentBranch: String): RIO[Git with Blocking, VersionData] = {
    for {
      commitInfos <- getCommitInfos(config.prefix)
      lastVersionOpt = getLastVersion(commitInfos)
      bumps <- getVersionBumps(lastVersionOpt)
      nextVersion = lastVersionOpt.map(_.version.bump(bumps)).getOrElse(Version())
    } yield {
      VersionData(
        major = nextVersion.major,
        minor = nextVersion.minor,
        patch = nextVersion.patch,
        commitCount = lastVersionOpt.map(_.commitsBeforeHead).getOrElse(commitInfos.length),
        branch = currentBranch,
        commitHashShort = commitInfos.headOption.map(_.shortHash).getOrElse(""),
        commitHashFull = commitInfos.headOption.map(_.fullHash).getOrElse(""),
        date = LocalDate.now(),
        buildNo = sys.env.getOrElse("BUILD_BUILDNUMBER", "TODO")
      )
    }
  }

  def getVersionBumps(lastVersion: Option[LastVersion]): RIO[Git with Blocking, VersionBumps] = {
    lastVersion match {
      case None => RIO.succeed(VersionBumps.minVersionBump) // No previous version
      case Some(LastVersion(_, 0, _)) => RIO.succeed(VersionBumps.none) // This commit is a version
      case Some(lv) => {
        Git.fullLog(lv.commitHash).map { log =>
          val logBumps: VersionBumps = calcBumps(log.linesIterator.toList, VersionBumps())
          if (logBumps == VersionBumps()) {
            VersionBumps.minVersionBump
          } else {
            logBumps
          }
        }
      }
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
