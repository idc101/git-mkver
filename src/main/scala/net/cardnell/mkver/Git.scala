package net.cardnell.mkver

import net.cardnell.mkver.ProcessUtils._
import zio.blocking.Blocking
import zio.process.Command
import zio.{Has, Layer, RIO, Task, ZIO, ZLayer}

object Git {
  trait Service {
    def currentBranch(): RIO[Blocking, String]
    def fullLog(fromRef: Option[String]): RIO[Blocking, String]
    def commitInfoLog(): RIO[Blocking, String]
    def tag(tag: String, tagMessage: String): RIO[Blocking, Unit]
    def checkGitRepo(): RIO[Blocking, Unit]
  }

  def live(workingDir: Option[File] = None): Layer[Nothing, Has[Service]] = ZLayer.succeed(
    new Service {
      val cwd: Option[File] = workingDir

      def currentBranch(): RIO[Blocking, String] = {
        if (sys.env.contains("BUILD_SOURCEBRANCH")) {
          // Azure Devops Pipeline
          RIO.succeed(sys.env("BUILD_SOURCEBRANCH")
            .replace("refs/heads/", "")
            .replace("refs/", ""))
        } else if (sys.env.contains("CI_COMMIT_REF_NAME")) {
          // Gitlab CI
          RIO.succeed(sys.env("CI_COMMIT_REF_NAME"))
        } else {
          // TODO better fallback if we in detached head mode like build systems do
          exec("git rev-parse --abbrev-ref HEAD", cwd).map(_.stdout)
        }
      }

      def commitInfoLog(): RIO[Blocking, String] = {
        exec(Array("git", "log", "--pretty=%h %H %d"), cwd).map(_.stdout)
      }

      def fullLog(fromRef: Option[String]): RIO[Blocking, String] = {
        val refRange = fromRef.map(r => Array(s"$r..HEAD")).getOrElse(Array())
        exec(Array("git", "--no-pager", "log") ++ refRange, cwd).map(_.stdout)
      }

      def tag(tag: String, tagMessage: String): RIO[Blocking, Unit] = {
        exec(Array("git", "tag", "-a", "-m", tagMessage, tag), cwd).unit
      }

      def checkGitRepo(): RIO[Blocking, Unit] = {
        exec(s"git --no-pager show", cwd).flatMap { output =>
          Task.fail(MkVerException(output.stdout)).when(output.exitCode != 0)
        }
      }
    }
  )

  //accessor methods
  def currentBranch(): ZIO[Git with Blocking, Throwable, String] =
    ZIO.accessM(_.get.currentBranch())

  def fullLog(fromRef: Option[String]): ZIO[Git with Blocking, Throwable, String] =
    ZIO.accessM(_.get.fullLog(fromRef))

  def commitInfoLog(): ZIO[Git with Blocking, Throwable, String] =
    ZIO.accessM(_.get.commitInfoLog())

  def tag(tag: String, tagMessage: String): RIO[Git with Blocking, Unit] =
    ZIO.accessM(_.get.tag(tag, tagMessage))

  def checkGitRepo(): RIO[Git with Blocking, Unit] =
    ZIO.accessM(_.get.checkGitRepo())
}

object ProcessUtils {
  def exec(command: String): ZIO[Blocking, MkVerException, ProcessResult] = {
    exec(command.split(" "), None)
  }

  def exec(command: String, dir: Option[File]): ZIO[Blocking, MkVerException, ProcessResult] = {
    exec(command.split(" "), dir)
  }

  def exec(command: String, dir: File): ZIO[Blocking, MkVerException, ProcessResult] = {
    exec(command.split(" "), Some(dir))
  }

  def exec(commands: Array[String], dir: Option[File] = None): ZIO[Blocking, MkVerException, ProcessResult] = {
    val processName = commands(0)
    val args = commands.tail
    val command = Command(processName, args:_*)
    val process = dir.map(d => command.workingDirectory(d.file))
      .getOrElse(command)
    val result = (for {
      p <- process.run
      lines <- p.stdout.string
      linesStdErr <- p.stderr.string
      exitCode <- p.exitCode
    } yield ProcessResult(lines.trim, linesStdErr.trim, exitCode.code)).mapError(ce => MkVerException(ce.toString))

    result.flatMap { pr =>
      if (pr.exitCode != 0) {
        ZIO.fail(MkVerException(s"Git error: ${pr.stderr}"))
      } else {
        ZIO.succeed(pr)
      }
    }
  }
}
