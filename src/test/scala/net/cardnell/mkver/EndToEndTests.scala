package net.cardnell.mkver

import better.files.File
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers


class EndToEndTests extends AnyFlatSpec with Matchers {
  "no tags" should "return version 0.1.0" in {
    File.usingTemporaryDirectory("git-mkver") { tempDir =>
      init(tempDir)
      fix("code1.py", tempDir)
      run(tempDir, "next") should be(Right("0.1.0"))
    }
  }

  "master advances correctly" should "return version 0.1.1" in {
    File.usingTemporaryDirectory("git-mkver") { tempDir =>
      init(tempDir)
      fix("code1.py", tempDir)
      run(tempDir, "tag")
      fix("code2.py", tempDir)
      run(tempDir, "next") should be(Right("0.1.1"))
      println(ProcessUtils.exec("git log --graph --full-history --color --oneline", Some(tempDir)).stdout)
    }
  }

  "feature branch (+minor) and master (+major) both advance version" should "return version 1.0.0" in {
    File.usingTemporaryDirectory("git-mkver") { tempDir =>
      init(tempDir)
      fix("code1.py", tempDir)
      run(tempDir, "tag")
      branch("feature/f1", tempDir)
      feat("code2.py", tempDir)
      checkout("master", tempDir)
      major("code3.py", tempDir)
      merge("feature/f1", tempDir)
      run(tempDir, "next") should be(Right("1.0.0"))
      println(ProcessUtils.exec("git log --graph --full-history --color --oneline", Some(tempDir)).stdout)
    }
  }

  "feature branch (+major) and master (+minor) both advance version" should "return version 1.0.0" in {
    File.usingTemporaryDirectory("git-mkver") { tempDir =>
      init(tempDir)
      fix("code1.py", tempDir)
      run(tempDir, "tag")
      branch("feature/f1", tempDir)
      major("code2.py", tempDir)
      checkout("master", tempDir)
      feat("code3.py", tempDir)
      merge("feature/f1", tempDir)
      run(tempDir, "next") should be(Right("1.0.0"))
      println(ProcessUtils.exec("git log --graph --full-history --color --oneline", Some(tempDir)).stdout)
    }
  }

  "feature branch 1 (+major) and feature branch 2 (+minor) both advance version" should "return version 1.0.0" in {
    File.usingTemporaryDirectory("git-mkver") { tempDir =>
      init(tempDir)
      fix("code1.py", tempDir)
      run(tempDir, "tag")

      branch("feature/f1", tempDir)
      feat("code2.py", tempDir)
      checkout("master", tempDir)

      branch("feature/f2", tempDir)
      major("code3.py", tempDir)
      checkout("master", tempDir)

      merge("feature/f1", tempDir)
      merge("feature/f2", tempDir)
      run(tempDir, "next") should be(Right("1.0.0"))
      println(ProcessUtils.exec("git log --graph --full-history --color --oneline", Some(tempDir)).stdout)
    }
  }

  def run(tempDir: File, command: String): Either[MkVerError, String] = {
    new Main(Git.Live.git(Some(tempDir))).mainImpl(Array(command))
  }

  def init(tempDir: File): Unit = {
    exec(Array("git", "init"), Some(tempDir))
    exec(Array("git", "config", "user.name", "Mona Lisa"), Some(tempDir))
    exec(Array("git", "config", "user.email", "mona.lisa@email.org"), Some(tempDir))
  }

  def fix(name: String, tempDir: File): Unit = {
    (tempDir / name).touch()
    exec(Array("git", "add", "."), Some(tempDir))
    exec(Array("git", "commit", "-m", s"fix: $name"), Some(tempDir))
  }

  def feat(name: String, tempDir: File): Unit = {
    (tempDir / name).touch()
    exec(Array("git", "add", "."), Some(tempDir))
    exec(Array("git", "commit", "-m", s"feat: $name"), Some(tempDir))
  }

  def major(name: String, tempDir: File): Unit = {
    (tempDir / name).touch()
    exec(Array("git", "add", "."), Some(tempDir))
    exec(Array("git", "commit", "-m", s"major: $name"), Some(tempDir))
  }

  def branch(name: String, tempDir: File): Unit = {
    exec(Array("git", "checkout", "-b", name), Some(tempDir))
  }

  def merge(name: String, tempDir: File): Unit = {
    exec(Array("git", "merge", "--no-ff", name), Some(tempDir))
  }

  def checkout(name: String, tempDir: File): Unit = {
    exec(Array("git", "checkout", name), Some(tempDir))
  }

  def exec(commands: Array[String], dir: Option[File] = None) = {
    val result = ProcessUtils.exec(commands, dir)
    if (result.exitCode != 0) {
      println(result.stderr)
      result.exitCode should be(0)
    }
  }
}

