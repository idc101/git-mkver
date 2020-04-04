package net.cardnell.mkver

import better.files.File
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import ProcessUtils._


class EndToEndTests extends AnyFlatSpec with Matchers {
  "no tags" should "return version 0.1.0" in {
    File.usingTemporaryDirectory("git-mkver") { tempDir =>
      init(tempDir)
      fix("code1.py", tempDir)
      new Main(Git.Live.git(Some(tempDir))).mainImpl(Array("next")) should be(Right("v0.1.0"))
    }
  }

  "master advances correctly" should "return version 0.1.1" in {
    File.usingTemporaryDirectory("git-mkver") { tempDir =>
      init(tempDir)
      fix("code1.py", tempDir)
      new Main(Git.Live.git(Some(tempDir))).mainImpl(Array("tag"))
      fix("code2.py", tempDir)
      new Main(Git.Live.git(Some(tempDir))).mainImpl(Array("next")) should be(Right("v0.1.1"))
    }
  }

  def init(tempDir: File): Unit = {
    exec("git init", tempDir)
    exec(Array("git", "config", "user.name", "Mona Lisa"), Some(tempDir))
    exec(Array("git", "config", "user.email", "mona.lisa@email.org"), Some(tempDir))
  }

  def fix(name: String, tempDir: File): Unit = {
    (tempDir / name).touch()
    exec(Array("git", "add", "."), Some(tempDir))
    exec(Array("git", "commit", "-m", s"fix: $name"), Some(tempDir))
  }
}

