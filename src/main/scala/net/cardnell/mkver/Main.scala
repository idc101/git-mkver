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
      config <- AppConfig.getRunConfig(opts.configFile, currentBranch)
      r <- opts.p match {
        case nextOps@NextOpts(_, _, _) =>
          runNext(nextOps, config, currentBranch)
        case tagOpts@TagOpts(_) =>
          runTag(tagOpts, config, currentBranch).map(_ => "")
        case patchOpts@PatchOpts(_) =>
          runPatch(patchOpts, config, currentBranch).map(_ => "")
        case infoOpts@InfoOpts(_, _) =>
          runInfo(infoOpts, config, currentBranch)
      }
    } yield r
  }

  def runNext(nextOpts: NextOpts, config: RunConfig, currentBranch: String): RIO[Git with Blocking, String] = {
    getNextVersion(config, currentBranch, nextOpts.preRelease).flatMap { nextVersion =>
      nextOpts.format.map { format =>
        Task.effect(Formatter(nextVersion, config, nextOpts.preRelease).format(format))
      }.getOrElse {
        formatVersion(config, nextVersion, nextOpts.prefix, nextOpts.preRelease)
      }
    }
  }

  def runTag(tagOpts: TagOpts, config: RunConfig, currentBranch: String) = {
    for {
      nextVersion <- getNextVersion(config, currentBranch, tagOpts.preRelease)
      tag <- formatVersion(config, nextVersion, formatAsTag = true, preRelease = tagOpts.preRelease)
      tagMessage = Formatter(nextVersion, config, tagOpts.preRelease).format(config.tagMessageFormat)
      _ <- if (config.tag && nextVersion.commitCount > 0) {
        Git.tag(tag, tagMessage)
      } else {
        RIO.unit
      }
    } yield ()
  }

  def runPatch(patchOpts: PatchOpts, config: RunConfig, currentBranch: String) = {
    getNextVersion(config, currentBranch, patchOpts.preRelease).flatMap { nextVersion =>
      ZIO.foreach(config.patches) { patch =>
        val regex = patch.find.r
        val replacement = Formatter(nextVersion, config, patchOpts.preRelease).format(patch.replace)
        ZIO.foreach(patch.filePatterns) { filePattern =>
          for {
            cwd <- Path.currentWorkingDirectory
            matches <- Files.glob(cwd, filePattern)
            l <- ZIO.foreach(matches) { fileMatch =>
              for {
                _ <- putStrLn(s"Patching file: '${fileMatch.path.toString}', new value: '$replacement'")
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

  def runInfo(infoOpts: InfoOpts, config: RunConfig, currentBranch: String): RIO[Git with Blocking, String] = {
    getNextVersion(config, currentBranch, infoOpts.preRelease).map { nextVersion =>
      val formatter = Formatter(nextVersion, config, infoOpts.preRelease)
      val formats = formatter.formats.map { format =>
        val result = formatter.format(format.format)
        s"${format.name}=$result"
      }.mkString(System.lineSeparator())

      if (infoOpts.includeBranchConfig) {
        config.toString + System.lineSeparator() + System.lineSeparator() + formats
      } else {
        formats
      }
    }
  }
}
