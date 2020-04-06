package net.cardnell.mkver

import ProcessUtils._
import zio.{RIO, Task}
import zio.blocking.Blocking
import zio.process.Command

trait Git {
  //val git: Git.Service
  def git(dir: Option[File] = None): Git.Service
}

object Git {
  trait Service {
    def currentBranch(): RIO[Blocking, String]
    def fullLog(lastVersionTag: String): RIO[Blocking, String]
    def commitInfoLog(): RIO[Blocking, String]
    def tag(tag: String, tagMessage: String): RIO[Blocking, Unit]
    def checkGitRepo(): RIO[Blocking, Unit]
  }

  trait Live extends Git {
    def git(dir: Option[File] = None): Service = new Service {
      val cwd: Option[File] = dir

      def currentBranch(): RIO[Blocking, String] = {
        if (sys.env.contains("BUILD_SOURCEBRANCH")) {
          // Azure Devops Pipeline
          RIO.succeed(sys.env("BUILD_SOURCEBRANCH")
            .replace("refs/heads/", "")
            .replace("refs/", ""))
        } else {
          // TODO better fallback if we in detached head mode like build systems do
          exec("git rev-parse --abbrev-ref HEAD", cwd).map(_.stdout)
        }
      }

      def commitInfoLog(): RIO[Blocking, String] = {
        exec(Array("git", "log", "--pretty=%h %H %d"), cwd).map(_.stdout)
      }

      def fullLog(lastVersionTag: String): RIO[Blocking, String] = {
        exec(s"git --no-pager log $lastVersionTag..HEAD", cwd).map(_.stdout)
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
  }
  object Live extends Live
}

object ProcessUtils {
  def exec(command: String): RIO[Blocking, ProcessResult] = {
    exec(command.split(" "), None)
  }

  def exec(command: String, dir: Option[File]): RIO[Blocking, ProcessResult] = {
    exec(command.split(" "), dir)
  }

  def exec(command: String, dir: File): RIO[Blocking, ProcessResult] = {
    exec(command.split(" "), Some(dir))
  }

  def exec(commands: Array[String], dir: Option[File] = None): RIO[Blocking, ProcessResult] = {
    val processName = commands(0)
    val args = commands.tail
    val command = Command(processName, args:_*)
    val process = dir.map(d => command.workingDirectory(d.file))
      .getOrElse(command)
    for {
      p <- process.run
      lines <- p.string
      exitCode <- p.exitCode
    } yield ProcessResult(lines.trim, "", exitCode)
  }
}
