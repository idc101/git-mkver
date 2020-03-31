package net.cardnell.mkver

import better.files._
import net.cardnell.mkver.MkVer._

import net.cardnell.mkver.CommandLineArgs.{CiOpts, NextOpts, PatchOpts, TagOpts}


case class ProcessResult(stdout: String, stderr: String, exitCode: Int)

object Main {

  val patchConfigs = List(
    PatchConfig("helm-chart", List("**/Chart.yaml"), "version: .*", "version: \"%ver\""),
    PatchConfig("csproj", List("**/*.csproj"), "<Version>.*</Version>", "<Version>%ver</Version>")
  )

  def main(args: Array[String]): Unit = {
    CommandLineArgs.mkverCommand.parse(args, sys.env) match {
      case Left(help) =>
        System.err.println(help)
        sys.exit(1)
      case Right(NextOpts(_)) =>
        runNext()
      case Right(TagOpts(_)) =>
        runTag()
      case Right(PatchOpts(_)) =>
        runPatch()
      case Right(CiOpts(_)) =>
        runTag()
        runPatch()
    }

    sys.exit(0)
  }

  def runNext(): Unit = {
    checkGitRepo()
    val currentBranch = exec("git rev-parse --abbrev-ref HEAD").stdout
    val config = getConfig(currentBranch)
    val nextVersionData = getNextVersion(config, currentBranch)
    println(formatTag(config, nextVersionData))
  }

  def runTag(): Unit = {
    checkGitRepo()
    val currentBranch = exec("git rev-parse --abbrev-ref HEAD").stdout
    val config = getConfig(currentBranch)
    val nextVersion = getNextVersion(config, currentBranch)
    val tag = formatTag(config, nextVersion)
    val tagMessage = VariableReplacer(nextVersion).replace(config.tagMessageFormat)
    if (config.tag && nextVersion.commitCount > 0) {
      exec(Array("git", "tag", "-a", "-m", tagMessage, tag))
    }
  }

  def runPatch() = {
    val currentBranch = exec("git rev-parse --abbrev-ref HEAD").stdout
    val config = getConfig(currentBranch)
    val nextVersion = getNextVersion(config, currentBranch)
    patchConfigs.foreach { patch =>
      val regex = patch.findRegex.r
      val replacement = VariableReplacer(nextVersion).replace(patch.replace)
      println(replacement)
      patch.filePatterns.foreach { filePattern =>
        File.currentWorkingDirectory.glob(filePattern, includePath = false).foreach { file =>
          println(s"checking $file")
          val newLines = file.lines().map { line =>
            regex.replaceAllIn(line, replacement)
          }
          file.overwrite(newLines.mkString(System.lineSeparator()))
        }
      }
    }
  }

  def checkGitRepo(): Unit = {
    val output = exec(s"git show")
    if (output.exitCode != 0) {
      System.err.println(output.stderr)
      System.exit(output.exitCode)
    }
  }

  def getConfig(currentBranch: String): BranchConfig = {
    if (currentBranch == "master") {
      BranchConfig("master".r, "v", true, TagParts.Version, "release %v", "rc", "%sh", patchConfigs)
    } else {
      BranchConfig(".*".r, "v", false, TagParts.VersionBuildMetadata, "release %v", "rc", "%br.%sh", patchConfigs)
    }
  }
}
