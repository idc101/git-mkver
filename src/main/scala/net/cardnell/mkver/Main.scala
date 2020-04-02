package net.cardnell.mkver

import better.files._
import net.cardnell.mkver.MkVer._

import net.cardnell.mkver.CommandLineArgs.{CiOpts, NextOpts, PatchOpts, TagOpts}

case class ProcessResult(stdout: String, stderr: String, exitCode: Int)

object Main {
  def main(args: Array[String]): Unit = {
    new Main().mainImpl(args)
  }
}

class Main(git: Git.Service = Git.Live.git()) {
  def mainImpl(args: Array[String]): Unit = {
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
  }

  def runNext(nextOpts: NextOpts): Unit = {
    git.checkGitRepo()
    val currentBranch = git.currentBranch()
    val config = AppConfig.getBranchConfig(currentBranch)
    val nextVersionData = getNextVersion(git, config, currentBranch)
    val output = nextOpts.format.map { format =>
      Formatter(nextVersionData, config).format(format)
    }.getOrElse(formatTag(config, nextVersionData))
    println(output)
  }

  def runTag(): Unit = {
    git.checkGitRepo()
    val currentBranch = git.currentBranch()
    val config = AppConfig.getBranchConfig(currentBranch)
    val nextVersion = getNextVersion(git, config, currentBranch)
    val tag = formatTag(config, nextVersion)
    val tagMessage = Formatter(nextVersion, config).format(config.tagMessageFormat)
    if (config.tag && nextVersion.commitCount > 0) {
      git.tag(tag, tagMessage)
    }
  }

  def runPatch(): Unit = {
    val currentBranch = git.currentBranch()
    val config = AppConfig.getBranchConfig(currentBranch)
    val nextVersion = getNextVersion(git, config, currentBranch)
    AppConfig.getPatchConfigs(config).foreach { patch =>
      val regex = patch.find.r
      val replacement = Formatter(nextVersion, config).format(patch.replace)
      patch.filePatterns.foreach { filePattern =>
        File.currentWorkingDirectory.glob(filePattern, includePath = false).foreach { file =>
          println(s"patching: $file, replacement: $replacement")
          val newContent = regex.replaceAllIn(file.contentAsString, replacement)
          file.overwrite(newContent)
        }
      }
    }
  }
}
