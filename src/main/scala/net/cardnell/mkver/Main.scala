package net.cardnell.mkver

import net.cardnell.mkver.MkVer._
import net.cardnell.mkver.CommandLineArgs.{CommandLineOpts, InfoOpts, NextOpts, PatchOpts, TagOpts}
import zio._
import zio.blocking.Blocking
import zio.console._

case class ProcessResult(stdout: String, stderr: String, exitCode: Int)

case class MkVerException(message: String) extends Exception {
  override def getMessage: String = message
}

object Main extends App {
  def run(args: List[String]) =
    appLogic(args).fold(_ => 1, _ => 0)

  def appLogic(args: List[String]) = {
    new Main().mainImpl(args)
      .flatMap(message => putStrLn(message))
      .flatMapError(err => putStrLn(err.getMessage))
  }
}

class Main(git: Git.Service = Git.Live.git()) {
  def mainImpl(args: List[String]): RIO[Blocking, String] = {
    CommandLineArgs.mkverCommand.parse(args, sys.env)
      .fold( help => Task.fail(MkVerException(help.toString())), opts => run(opts))
  }

  def run(opts: CommandLineOpts): RIO[Blocking, String] = {
    for {
      _ <- git.checkGitRepo()
      currentBranch <- git.currentBranch()
      config <- AppConfig.getBranchConfig(opts.configFile, currentBranch)
      r <- opts.p match {
        case nextOps@NextOpts(_, _) =>
          runNext(nextOps, config, currentBranch)
        case TagOpts(_) =>
          runTag(config, currentBranch).map(_ => "")
        case PatchOpts(_) =>
          AppConfig.getPatchConfigs(opts.configFile, config).flatMap { patchConfigs =>
            runPatch(config, currentBranch, patchConfigs).map(_ => "")
          }
        case InfoOpts(includeBranchConfig) =>
          runInfo(config, currentBranch, includeBranchConfig)
      }
    } yield r
  }

  def runNext(nextOpts: NextOpts, config: BranchConfig, currentBranch: String): RIO[Blocking, String] = {
    getNextVersion(git, config, currentBranch).flatMap { nextVersion =>
      nextOpts.format.map { format =>
        Task.effect(Formatter(nextVersion, config).format(format))
      }.getOrElse {
        formatTag(config, nextVersion, nextOpts.prefix)
      }
    }
  }

  def runTag(config: BranchConfig, currentBranch: String) = {
    for {
      nextVersion <- getNextVersion(git, config, currentBranch)
      tag <- formatTag(config, nextVersion)
      tagMessage = Formatter(nextVersion, config).format(config.tagMessageFormat)
      _ <- if (config.tag && nextVersion.commitCount > 0) {
        git.tag(tag, tagMessage)
      } else {
        RIO.unit
      }
    } yield ()
  }

  def runPatch(config: BranchConfig, currentBranch: String, patchConfigs: List[PatchConfig]): RIO[Blocking, Unit] = {
    getNextVersion(git, config, currentBranch).map { nextVersion =>
      patchConfigs.foreach { patch =>
        val regex = patch.find.r
        val replacement = Formatter(nextVersion, config).format(patch.replace)
        patch.filePatterns.foreach { filePattern =>
          for {
            cwd <- Files.currentWorkingDirectory
            matches <- Files.glob(cwd, filePattern)
            _ <- matches.foreach { fileMatch =>
              // TODO replace as entire string rather than lines to preserve line endings
              for {
                content <- Files.readAllLines(fileMatch)
                newContent <- ZIO.effect(content.map(l => regex.replaceAllIn(l, replacement)))
                _ <- Files.write(fileMatch, newContent)
              } yield ()
            }
          } yield ()
//          File.currentWorkingDirectory.glob(filePattern, includePath = true).foreach { file =>
//            println(s"patching: $file, replacement: $replacement")
//            val newContent = regex.replaceAllIn(file.contentAsString, replacement)
//            file.overwrite(newContent)
//          }
        }
      }
    }
  }

  def runInfo(config: BranchConfig, currentBranch: String, includeBranchConfig: Boolean): RIO[Blocking, String] = {
    getNextVersion(git, config, currentBranch).map { nextVersion =>
      val formatter = Formatter(nextVersion, config)
      val formats = formatter.formats.map { format =>
        val result = formatter.format(format.format)
        s"${format.name}=$result"
      }.mkString(System.lineSeparator())

      if (includeBranchConfig) {
        config.toString + System.lineSeparator() + System.lineSeparator() + formats
      } else {
        formats
      }
    }
  }
}
