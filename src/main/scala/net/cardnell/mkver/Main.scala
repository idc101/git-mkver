package net.cardnell.mkver

import better.files._
import net.cardnell.mkver.MkVer._
import net.cardnell.mkver.CommandLineArgs.{CommandLineOpts, NextOpts, PatchOpts, TagOpts}

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
      case Right(opts) =>
        run(opts)
    }
  }

  def run(opts: CommandLineOpts): Unit = {
    git.checkGitRepo()
    val currentBranch = git.currentBranch()
    val config = AppConfig.getBranchConfig(opts.configFile, currentBranch)
    opts.p match {
      case nextOps@NextOpts(_) =>
        runNext(nextOps, config, currentBranch)
      case TagOpts(_) =>
        runTag(config, currentBranch)
      case PatchOpts(_) =>
        val patchConfigs = AppConfig.getPatchConfigs(opts.configFile, config)
        runPatch(config, currentBranch, patchConfigs)
    }
  }

  def runNext(nextOpts: NextOpts, config: BranchConfig, currentBranch: String): Unit = {
    val nextVersionData = getNextVersion(git, config, currentBranch)
    val output = nextOpts.format.map { format =>
      Formatter(nextVersionData, config).format(format)
    }.getOrElse(formatTag(config, nextVersionData))
    println(output)
  }

  def runTag(config: BranchConfig, currentBranch: String): Unit = {
    val nextVersion = getNextVersion(git, config, currentBranch)
    val tag = formatTag(config, nextVersion)
    val tagMessage = Formatter(nextVersion, config).format(config.tagMessageFormat)
    if (config.tag && nextVersion.commitCount > 0) {
      git.tag(tag, tagMessage)
    }
  }

  def runPatch(config: BranchConfig, currentBranch: String, patchConfigs: List[PatchConfig]): Unit = {
    val nextVersion = getNextVersion(git, config, currentBranch)
    patchConfigs.foreach { patch =>
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
