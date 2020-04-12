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
  def run(args: List[String]): ZIO[ZEnv, Nothing, Int] =
    appLogic(args)
      .provideCustomLayer(Blocking.live >>> Git.live(None))
      .fold(_ => 1, _ => 0)

  def appLogic(args: List[String]) = {
    mainImpl(args)
      .flatMap(message => putStrLn(message))
      .flatMapError(err => putStrLn(err.getMessage))
  }

  def mainImpl(args: List[String]) = {
    CommandLineArgs.mkverCommand.parse(args, sys.env)
      .fold( help => Task.fail(MkVerException(help.toString())), opts => run(opts))
  }

  def run(opts: CommandLineOpts) = {
    for {
      _ <- Git.checkGitRepo()
      currentBranch <- Git.currentBranch()
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

  def runNext(nextOpts: NextOpts, config: BranchConfig, currentBranch: String): RIO[Git with Blocking, String] = {
    getNextVersion(config, currentBranch).flatMap { nextVersion =>
      nextOpts.format.map { format =>
        Task.effect(Formatter(nextVersion, config).format(format))
      }.getOrElse {
        formatTag(config, nextVersion, nextOpts.prefix)
      }
    }
  }

  def runTag(config: BranchConfig, currentBranch: String) = {
    for {
      nextVersion <- getNextVersion(config, currentBranch)
      tag <- formatTag(config, nextVersion)
      tagMessage = Formatter(nextVersion, config).format(config.tagMessageFormat)
      _ <- if (config.tag && nextVersion.commitCount > 0) {
        Git.tag(tag, tagMessage)
      } else {
        RIO.unit
      }
    } yield ()
  }

  def runPatch(config: BranchConfig, currentBranch: String, patchConfigs: List[PatchConfig]) = {
    getNextVersion(config, currentBranch).flatMap { nextVersion =>
      ZIO.foreach(patchConfigs) { patch =>
        val regex = patch.find.r
        val replacement = Formatter(nextVersion, config).format(patch.replace)
        ZIO.foreach(patch.filePatterns) { filePattern =>
          for {
            cwd <- Path.currentWorkingDirectory
            matches <- Files.glob(cwd, filePattern)
            l <- ZIO.foreach(matches) { fileMatch =>
              for {
                _ <- putStrLn(s"Patching $fileMatch with new value $replacement")
                content <- Files.readAll(fileMatch)
                newContent <- ZIO.effect(regex.replaceAllIn(content, replacement))
                p <- Files.write(fileMatch, newContent)
              } yield p
            }
          } yield l
        }
      }
    }.unit
  }

  def runInfo(config: BranchConfig, currentBranch: String, includeBranchConfig: Boolean): RIO[Git with Blocking, String] = {
    getNextVersion(config, currentBranch).map { nextVersion =>
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
