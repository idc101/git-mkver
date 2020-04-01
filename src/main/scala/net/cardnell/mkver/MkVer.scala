package net.cardnell.mkver

import java.io.{BufferedReader, InputStreamReader}
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
      case VersionBumps(true, _, _, _) => this.copy(major = this.major + 1)
      case VersionBumps(false, true, _, _) => this.copy(minor = this.minor + 1)
      case VersionBumps(false, false, true, _) => this.copy(patch = this.patch + 1)
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
  def getDescribeInfo(prefix: String): DescribeInfo = {
    val describeResult = exec(s"git describe --long --match=$prefix*")

    if (describeResult.exitCode != 0) {
      // No tags yet, assume default version
      DescribeInfo("v0.0.0", 1, exec("git rev-parse --short HEAD").stdout)
    } else {
      // what if it was a branch tag that contains "-"?
      val describe = "^(.*)-(\\d+)-g([0-9a-f]{5,40})$".r

      describeResult.stdout match {
        case describe(tag, commitCount, hash) => DescribeInfo(tag, commitCount.toInt, hash)
      }
    }
  }

  def getLastVersion(prefix: String, lastVersionTag: String): Version = {
    val version = ("^" + prefix + "(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-((?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\\+([0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?$").r

    lastVersionTag match {
      case version(major, minor, patch, prerelease, buildmetadata) => Version(major.toInt, minor.toInt, patch.toInt, Option(prerelease), Option(buildmetadata))
      case _ =>
        System.err.println(s"warning: unable to parse last tag. ($lastVersionTag) doesn't match a SemVer pattern")
        System.exit(1)
        Version(0, 0, 0, None, None)
    }
  }

  def formatTag(config: BranchConfig, versionData: VersionData): String = {
    val version = s"${config.prefix}${versionData.major}.${versionData.minor}.${versionData.patch}"
    val preRelease = "TODO"
    val buildMetaData = VariableReplacer(versionData).replace(config.buildMetadataFormat)
    config.tagParts match {
      case TagParts.Version => version
      //case TagParts.VersionPreRelease => s"$version-$preRelease"
      case TagParts.VersionBuildMetadata =>s"$version+$buildMetaData"
      //case TagParts.VersionPreReleaseBuildMetadata =>s"$version-$preRelease+$buildMetaData"
    }
  }

  def getNextVersion(config: BranchConfig, currentBranch: String): VersionData = {
    val describeInfo = getDescribeInfo(config.prefix)
    val lastVersion = getLastVersion(config.prefix, describeInfo.lastTag)
    // If no commits since last tag then there is no next version yet - make bumps = 0
    val bumps = if (describeInfo.commitCount == 0) VersionBumps() else getVersionBumps(describeInfo.lastTag)
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

  def getVersionBumps(lastVersionTag: String): VersionBumps = {
    val log = exec(s"git log $lastVersionTag..HEAD").stdout

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
        } else if (feat.findFirstIn(line).nonEmpty) {
          calcBumps(lines.tail, bumps.bumpMinor())
        } else if (fix.findFirstIn(line).nonEmpty) {
          calcBumps(lines.tail, bumps.bumpPatch())
        } else {
          calcBumps(lines.tail, bumps)
        }
      } else {
        calcBumps(lines.tail, bumps)
      }
    }
  }

  def getCurrentBranch(): String = {
    if (sys.env.contains("BUILD_SOURCEBRANCH")) {
      // Azure Devops Pipeline
      sys.env("BUILD_SOURCEBRANCH").replace("refs/heads/", "")
    } else {
      // TODO better fallback if we in detached head mode like build systems do
      exec("git rev-parse --abbrev-ref HEAD").stdout
    }
  }

  def exec(command: String): ProcessResult = {
    exec(command.split(" "))
  }

  def exec(commands: Array[String]): ProcessResult = {
    val runtime = Runtime.getRuntime
    val process = runtime.exec(commands)

    process.waitFor()

    val lineReader = new BufferedReader(new InputStreamReader(process.getInputStream))
    val stdout = lineReader.lines().toArray.mkString(System.lineSeparator())

    val errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream))
    val stderr = errorReader.lines().toArray.mkString(System.lineSeparator())

    ProcessResult(stdout, stderr, process.exitValue())
  }
}
