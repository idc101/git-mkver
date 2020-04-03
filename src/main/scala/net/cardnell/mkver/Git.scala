package net.cardnell.mkver

import java.io.{BufferedReader, InputStreamReader}

import better.files.File
import ProcessUtils._

trait Git {
  //val git: Git.Service
  def git(dir: Option[File] = None): Git.Service
}

object Git {
  trait Service {
    def currentBranch(): String
    def log(lastVersionTag: String): String
    def describe(prefix: String): String
    def tag(tag: String, tagMessage: String): Unit
    def checkGitRepo(): Unit
  }

  trait Live extends Git {
    def git(dir: Option[File] = None): Service = new Service {
      val cwd = dir

      def currentBranch(): String = {
        if (sys.env.contains("BUILD_SOURCEBRANCH")) {
          // Azure Devops Pipeline
          sys.env("BUILD_SOURCEBRANCH")
            .replace("refs/heads/", "")
            .replace("refs/", "")
        } else {
          // TODO better fallback if we in detached head mode like build systems do
          exec("git rev-parse --abbrev-ref HEAD", cwd).stdout
        }
      }

      def log(lastVersionTag: String): String = {
        exec(s"git --no-pager log $lastVersionTag..HEAD", cwd).stdout
      }

      def describe(prefix: String): String = {
        val describeResult = exec(s"git describe --long --match=$prefix*", cwd)

        if (describeResult.exitCode != 0) {
          // No tags yet, fake one
          val shortHash = exec("git rev-parse --short HEAD", cwd).stdout
          s"v0.0.0-1-g$shortHash"
        } else {
          describeResult.stdout
        }
      }

      def tag(tag: String, tagMessage: String): Unit = {
        exec(Array("git", "tag", "-a", "-m", tagMessage, tag), cwd)
      }

      def checkGitRepo(): Unit = {
        val output = exec(s"git --no-pager show", cwd)
        if (output.exitCode != 0) {
          System.err.println(output.stderr)
          System.exit(output.exitCode)
        }
      }
    }
  }
  object Live extends Live
}

object ProcessUtils {
  def exec(command: String): ProcessResult = {
    exec(command.split(" "), None)
  }

  def exec(command: String, dir: Option[File]): ProcessResult = {
    exec(command.split(" "), dir)
  }

  def exec(command: String, dir: File): ProcessResult = {
    exec(command.split(" "), Some(dir))
  }

  def exec(commands: Array[String], dir: Option[File] = None): ProcessResult = {
    val runtime = Runtime.getRuntime
    val process = dir.map(d => runtime.exec(commands, Array[String](), d.toJava))
      .getOrElse(runtime.exec(commands))

    val lineReader = new BufferedReader(new InputStreamReader(process.getInputStream))
    val stdout = lineReader.lines().toArray.mkString(System.lineSeparator())

    val errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream))
    val stderr = errorReader.lines().toArray.mkString(System.lineSeparator())

    process.waitFor()

    ProcessResult(stdout, stderr, process.exitValue())
  }
}