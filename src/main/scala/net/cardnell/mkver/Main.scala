package net.cardnell.mkver

import better.files._
import net.cardnell.mkver.MkVer._

import net.cardnell.mkver.CommandLineArgs.{CiOpts, NextOpts, PatchOpts, TagOpts}


case class ProcessResult(stdout: String, stderr: String, exitCode: Int)

object Main {

  def main(args: Array[String]): Unit = {
    CommandLineArgs.mkverCommand.parse(args, sys.env) match {
      case Left(help) =>
        System.err.println(help)
        sys.exit(1)
      case Right(nextOps@NextOpts(_)) =>
        runNext(nextOps)
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

  def runNext(nextOpts: NextOpts): Unit = {
    checkGitRepo()
    val currentBranch = getCurrentBranch()
    val config = AppConfig.getBranchConfig(currentBranch)
    val nextVersionData = getNextVersion(config, currentBranch)
    val output = nextOpts.format.map { format =>
      VariableReplacer(nextVersionData).replace(format)
    }.getOrElse(formatTag(config, nextVersionData))
    println(output)
  }

  def runTag(): Unit = {
    checkGitRepo()
    val currentBranch = getCurrentBranch()
    val config = AppConfig.getBranchConfig(currentBranch)
    val nextVersion = getNextVersion(config, currentBranch)
    val tag = formatTag(config, nextVersion)
    val tagMessage = VariableReplacer(nextVersion).replace(config.tagMessageFormat)
    if (config.tag && nextVersion.commitCount > 0) {
      exec(Array("git", "tag", "-a", "-m", tagMessage, tag))
    }
  }

  def runPatch(): Unit = {
    val currentBranch = getCurrentBranch()
    val config = AppConfig.getBranchConfig(currentBranch)
    val nextVersion = getNextVersion(config, currentBranch)
    AppConfig.getPatchConfigs(config).foreach { patch =>
      val regex = patch.find.r
      val replacement = VariableReplacer(nextVersion).replace(patch.replace)
      patch.filePatterns.foreach { filePattern =>
        File.currentWorkingDirectory.glob(filePattern, includePath = false).foreach { file =>
          println(s"patching: $file, replacement: $replacement")
          val newContent = regex.replaceAllIn(file.contentAsString, replacement)
          file.overwrite(newContent)
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
}
