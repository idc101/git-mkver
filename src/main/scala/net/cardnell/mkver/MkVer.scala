package net.cardnell.mkver

import java.time.LocalDate

import zio.blocking.Blocking
import zio.{IO, RIO, Task}

object MkVer {
  case class CommitInfo(shortHash: String, fullHash: String, commitsBeforeHead: Int, tags: List[Version])

  case class LastVersion(commitHash: String, commitsBeforeHead: Int, version: Version)

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
    val allowedFormats = Formatter.versionFormats.map(_.name)
    if (!allowedFormats.contains(config.versionFormat)) {
      IO.fail(MkVerException(s"versionFormat (${config.versionFormat}) must be one of: ${allowedFormats.mkString(", ")}"))
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
      commitInfos <- getCommitInfos(config.tagPrefix)
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
