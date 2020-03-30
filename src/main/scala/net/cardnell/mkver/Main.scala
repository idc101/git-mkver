package net.cardnell.mkver

import java.io.BufferedReader
import java.io.InputStreamReader

import com.monovore.decline._

case class Sample(hello: String, quiet: Boolean)

case class ProcessResult(stdout: String, stderr: String, exitCode: Int)

case class Version(major: Int = 0, minor: Int = 1, patch: Int = 0, prerelease: Option[String] = None, buildmetadata: Option[String] = None) {
  def bump(bumps: VersionBumps): Version = {
    bumps match {
      case VersionBumps(true, _, _, _) => this.copy(major = this.major + 1)
      case VersionBumps(false, true, _, _) => this.copy(minor = this.minor + 1)
      case VersionBumps(false, false, true, _) => this.copy(patch = this.patch + 1)
      case _ => this
    }
  }
  override def toString: String = s"$major.$minor.$patch" //-${major}+${major}."
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

case class NextOpts(format: Option[String])
case class TagOpts(format: Option[String])

object Main {
  val format: Opts[Option[String]] = Opts.option[String]("format", short = "f", metavar = "string", help = "Format string for the version number").orNone
  //val port = Opts.env[Int]("PORT", help = "The port to run on.")
  val nextOptions: Opts[NextOpts] = format.map(NextOpts.apply)
  val tagOptions: Opts[TagOpts] = format.map(TagOpts.apply)

  val nextCommand: Command[NextOpts] = Command("next", header = "Print the next version tag that would be used") {
    nextOptions
  }

  val tagCommand: Command[TagOpts] = Command("tag", header = "Git Tag the current commit with the next version tag") {
    tagOptions
  }

  val commands: Opts[Product] = Opts.subcommands(nextCommand, tagCommand)

  val mkverCommand: Command[Product] = Command(
    name = "git-mkver",
    header = "Uses git tags, branch names and commit messages to determine the next version of the software to release"
  ) {
    commands
  }



  def main(args: Array[String]): Unit = {
    val prefix = "v"

    mkverCommand.parse(args, sys.env) match {
      case Left(help) =>
        System.err.println(help)
        sys.exit(1)
      case Right(NextOpts(_)) =>
        runNext(prefix)
      case Right(TagOpts(_)) =>
        println("TODO")
    }

    sys.exit(0)
  }

  def runNext(prefix: String): Unit = {
    checkGitRepo()
    val describeInfo = getDescribeInfo(prefix)
    val lastVersion = getLastVersion(prefix, describeInfo.lastTag)
    // If no commits since last tag then there is no next version yet - make bumps = 0
    val bumps = if (describeInfo.commitCount == 0) VersionBumps() else getVersionBumps(describeInfo.lastTag)
    val nextVersion = lastVersion.bump(bumps)
    val currentBranch = exec("git rev-parse --abbrev-ref HEAD").stdout
    val currentBranchSafe = currentBranch.replace("/", "-")
    if (currentBranch == "master") {
      println(nextVersion)
    } else {
      println(s"$nextVersion+$currentBranchSafe.${describeInfo.commitHash}")
    }
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

  def checkGitRepo(): Unit = {
    val output = exec(s"git show")
    if (output.exitCode != 0) {
      System.err.println(output.stderr)
      System.exit(output.exitCode)
    }
  }

  def getDescribeInfo(prefix: String): DescribeInfo = {
    val describeResult = exec(s"git describe --tags --long --match=$prefix*")

    if (describeResult.exitCode != 0) {
      // No tags yet, assume default version
      DescribeInfo("v0.0.0", 0, exec("git rev-parse --short HEAD").stdout)
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
    }
  }

  def calcBumps(lines: List[String], bumps: VersionBumps): VersionBumps = {
    if (lines.isEmpty) {
      bumps
    } else {
      val line = lines.head
      if (line.startsWith("commit")) {
        calcBumps(lines.tail, bumps.bumpPatch())
      } else if (line.startsWith("    ")) {
        if (line.contains("fix:")) {
          calcBumps(lines.tail, bumps.bumpPatch())
        } else if (line.contains("feat:")) {
          calcBumps(lines.tail, bumps.bumpMinor())
        } else {
          calcBumps(lines.tail, bumps)
        }
      } else {
        calcBumps(lines.tail, bumps)
      }
    }
  }

  def exec(command: String): ProcessResult = {
    val runtime = Runtime.getRuntime
    val commands = command.split(" ")
    val process = runtime.exec(commands)

    process.waitFor()

    val lineReader = new BufferedReader(new InputStreamReader(process.getInputStream))
    val stdout = lineReader.lines().toArray.mkString(System.lineSeparator())

    val errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream))
    val stderr = errorReader.lines().toArray.mkString(System.lineSeparator())

    ProcessResult(stdout, stderr, process.exitValue())
  }

}
