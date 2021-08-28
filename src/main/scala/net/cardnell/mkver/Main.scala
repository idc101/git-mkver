package net.cardnell.mkver

import net.cardnell.mkver.MkVer._
import net.cardnell.mkver.CommandLineArgs.{CommandLineOpts, ConfigOpts, InfoOpts, NextOpts, PatchOpts, TagOpts}
import zio._
import zio.blocking.Blocking
import zio.console._

case class ProcessResult(stdout: String, stderr: String, exitCode: Int)

case class MkVerException(message: String) extends Exception {
  override def getMessage: String = message
}

object Main extends App {
  def run(args: List[String]): ZIO[ZEnv, Nothing, ExitCode] =
    appLogic(args)
      .provideCustomLayer(Blocking.live >>> Git.live(None))
      .fold(_ => ExitCode.failure, _ => ExitCode.success)

  def appLogic(args: List[String]): ZIO[Console with Git with Blocking, Unit, Unit] = {
    mainImpl(args)
      .flatMapError(err => putStrLn(err.getMessage))
  }

  def mainImpl(args: List[String]): ZIO[Console with Git with Blocking, Throwable, Unit] = {
    CommandLineArgs.mkverCommand.parse(args, sys.env)
      .fold( help => Task.fail(MkVerException(help.toString())), opts => run(opts))
  }

  def run(opts: CommandLineOpts): ZIO[Console with Git with Blocking, Throwable, Unit] = {
    for {
      _ <- Git.checkGitRepo()
      currentBranch <- Git.currentBranch()
      config <- AppConfig.getRunConfig(opts.configFile, currentBranch)
      r <- opts.opts match {
        case nextOps@NextOpts(_, _, _) =>
          runNext(nextOps, config, currentBranch)
        case tagOpts@TagOpts(_) =>
          runTag(tagOpts, config, currentBranch)
        case patchOpts@PatchOpts(_) =>
          runPatch(patchOpts, config, currentBranch)
        case infoOpts@InfoOpts(_, _) =>
          runInfo(infoOpts, config, currentBranch)
        case ConfigOpts =>
          runConfig(config)
      }
    } yield r
  }

  def runNext(nextOpts: NextOpts, config: RunConfig, currentBranch: String): ZIO[Console with Git with Blocking, Throwable, Unit] = {
    for {
      nextVersion <- getNextVersion(config, currentBranch, nextOpts.preRelease)
      next <- nextOpts.format.map { format =>
        Task.effect(Formatter(nextVersion, config, nextOpts.preRelease).format(format))
      }.getOrElse {
        formatVersion(config, nextVersion, nextOpts.prefix, nextOpts.preRelease)
      }
      _ <- putStrLn(next)
    } yield ()
  }

  def runTag(tagOpts: TagOpts, config: RunConfig, currentBranch: String): ZIO[Git with Blocking, Throwable, Unit] = {
    for {
      nextVersion <- getNextVersion(config, currentBranch, tagOpts.preRelease)
      tag <- formatVersion(config, nextVersion, formatAsTag = true, preRelease = tagOpts.preRelease)
      tagMessage = Formatter(nextVersion, config, tagOpts.preRelease).format(config.tagMessageFormat)
      _ <- Git.tag(tag, tagMessage) when (config.tag)
    } yield ()
  }

  def runPatch(patchOpts: PatchOpts, config: RunConfig, currentBranch: String): ZIO[Console with Git with Blocking, Throwable, Unit] = {
    getNextVersion(config, currentBranch, patchOpts.preRelease).flatMap { nextVersion =>
      ZIO.foreach(config.patches) { patch =>
        ZIO.foreach(patch.replacements) { findReplace =>
          val regex = findReplace.find
            .replace("{VersionRegex}", Version.versionFullRegex).r
          val replacement = Formatter(nextVersion, config, patchOpts.preRelease).format(findReplace.replace)
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
      }
    }.unit
  }

  def runInfo(infoOpts: InfoOpts, config: RunConfig, currentBranch: String): RIO[Console with Git with Blocking, Unit] = {
    for {
      nextVersion <- getNextVersion(config, currentBranch, infoOpts.preRelease)
      formatter = Formatter(nextVersion, config, infoOpts.preRelease)
      _ <- ZIO.foreach(formatter.formats) { format =>
        val result = formatter.format(format.format)
        putStrLn(s"${format.name}=$result") when (!format.name.startsWith("env") || infoOpts.includeEnv)
      }
    } yield ()
  }

  def runConfig(config: RunConfig): RIO[Console, Unit] = {
    import zio.config.typesafe._
    zio.config.write(AppConfig.runConfigDesc, config) match {
      case Left(s) => RIO.fail(MkVerException(s))
      case Right(pt) => putStrLn(pt.toHoconString)
    }
  }
}
