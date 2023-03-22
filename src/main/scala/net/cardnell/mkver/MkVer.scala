package net.cardnell.mkver

import java.time.LocalDate

import zio.blocking.Blocking
import zio.{IO, RIO, Task}

object MkVer {
  case class CommitInfo(shortHash: String, fullHash: String, commitsBeforeHead: Int, tags: List[Version])

  case class LastVersion(commitHash: String, commitsBeforeHead: Int, version: Version)

  def getCommitInfos(prefix: String): RIO[Git with Blocking, List[CommitInfo]] = {
    val lineMatch = "^([0-9a-f]{5,40}) ([0-9a-f]{5,40}) *(\\((.*)\\))?$".r

    Git.commitInfoLog().map { log =>
      log.linesIterator.zipWithIndex.flatMap {
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
    commitInfos.find(_.tags.nonEmpty).map(ci => LastVersion(ci.fullHash, ci.commitsBeforeHead, ci.tags.max))
  }

  def formatVersion(config: RunConfig, versionData: VersionData, formatAsTag: Boolean, preRelease: Boolean): Task[String] = {
    Task.effect {
      val formatter = Formatter(versionData, config, preRelease)
      if (formatAsTag) {
        formatter.format("{Tag}")
      } else {
        formatter.format("{Next}")
      }
    }
  }

  def getNextVersion(config: RunConfig, currentBranch: String, preRelease: Boolean): RIO[Git with Blocking, VersionData] = {
    for {
      commitInfos <- getCommitInfos(config.tagPrefix)
      lastVersionOpt = getLastVersion(commitInfos)
      bumps <- getVersionBumps(currentBranch, lastVersionOpt, config.commitMessageActions, config.whenNoValidCommitMessages)
      nextVersion = lastVersionOpt.map(_.version.getNextVersion(bumps, preRelease)).getOrElse(NextVersion(0, 1, 0, if (preRelease) Some(1) else None))
    } yield {
      VersionData(
        major = nextVersion.major,
        minor = nextVersion.minor,
        patch = nextVersion.patch,
        preReleaseNumber = nextVersion.preReleaseNumber,
        commitCount = lastVersionOpt.map(_.commitsBeforeHead).getOrElse(commitInfos.length),
        branch = currentBranch,
        commitHashShort = commitInfos.headOption.map(_.shortHash).getOrElse(""),
        commitHashFull = commitInfos.headOption.map(_.fullHash).getOrElse(""),
        date = LocalDate.now()
      )
    }
  }

  def getVersionBumps(currentBranch: String,
                      lastVersion: Option[LastVersion],
                      commitMessageActions: List[CommitMessageAction],
                      whenNoValidCommitMessages: IncrementAction): RIO[Git with Blocking, VersionBumps] = {
    def logToBumps(log: String): IO[MkVerException, VersionBumps] = {
      val logBumps: VersionBumps = calcBumps(log.linesIterator.toList, commitMessageActions, VersionBumps())
      if (logBumps.noValidCommitMessages()) {
        getFallbackVersionBumps(whenNoValidCommitMessages, logBumps)
      } else {
        RIO.succeed(logBumps)
      }
    }

    val bumps = lastVersion match {
      case None => Git.fullLog(None).flatMap(logToBumps) // No previous version
      case Some(LastVersion(_, 0, _)) => RIO.succeed(VersionBumps.none) // This commit is a version
      case Some(lv) => Git.fullLog(Some(lv.commitHash)).flatMap(logToBumps)
    }

    val releaseBranch = "^(hotfix|rel|release)[-/](\\d+\\.\\d+\\.\\d+)$".r
    val branchNameOverride = currentBranch match {
      case releaseBranch(_, v) => Version.parseTag(v, "")
      case _ => None
    }

    bumps.map(_.withBranchNameOverride(branchNameOverride))
  }

  def getFallbackVersionBumps(whenNoValidCommitMessages: IncrementAction, logBumps: VersionBumps): IO[MkVerException, VersionBumps] = {
    whenNoValidCommitMessages match {
      case IncrementAction.Fail => IO.fail(MkVerException("No valid commit messages found describing version increment"))
      case IncrementAction.NoIncrement => IO.succeed(logBumps)
      case other => IO.succeed(logBumps.bump(other))
    }
  }

  def calcBumps(lines: List[String], commitMessageActions: List[CommitMessageAction], bumps: VersionBumps): VersionBumps = {
    val overrideRegex = "    next-version: *(\\d+\\.\\d+\\.\\d+)".r
    if (lines.isEmpty) {
      bumps
    } else {
      val line = lines.head
      if (line.startsWith("commit")) {
        calcBumps(lines.tail, commitMessageActions, bumps.bumpCommits())
      } else if (line.startsWith("    ")) {
        // check for override text
        val newBumps = line match {
          case overrideRegex(v) => Version.parseTag(v, "").map(bumps.withCommitOverride).getOrElse(bumps)
          case _ => bumps
        }
        // check for bump messages
        val newBumps2 = commitMessageActions.flatMap { cma =>
          if (cma.pattern.r.findFirstIn(line).nonEmpty) {
            Some(newBumps.bump(cma.action))
          } else {
            None
          }
        }.headOption.getOrElse(newBumps)
        calcBumps(lines.tail, commitMessageActions, newBumps2)
      } else {
        calcBumps(lines.tail, commitMessageActions, bumps)
      }
    }
  }
}
