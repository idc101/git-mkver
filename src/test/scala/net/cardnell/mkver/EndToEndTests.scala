package net.cardnell.mkver

import net.cardnell.mkver.Main.mainImpl
import zio.blocking.Blocking
import zio.console.Console
import zio.test.Assertion._
import zio.test._
import zio.test.mock.MockConsole
import zio.{RIO, ULayer, ZIO}

object EndToEndTests extends DefaultRunnableSpec {
  def spec = suite("trunk based semver development")(
    testM("no tags should return version 0.1.0") {
      val mockEnv: ULayer[Console] = MockConsole.PutStrLn(equalTo("0.1.0"))
      val result = test { tempDir =>
        for {
          _ <- fix("code1.py", tempDir)
          run <- run(tempDir, "next").provideCustomLayer(mockEnv)
        } yield run
      }
      assertM(result)(isUnit)
    },
    testM("master advances correctly and should return version 0.1.1") {
      val mockEnv: ULayer[Console] = MockConsole.PutStrLn(equalTo("0.1.1"))
      val result = test { tempDir =>
        for {
          _ <- fix("code1.py", tempDir)
          _ <- run(tempDir, "tag")
          _ <- fix("code2.py", tempDir)
          //_ <- println(ProcessUtils.exec("git log --graph --full-history --color --oneline", Some(tempDir)).stdout)
          run <- run(tempDir, "next").provideCustomLayer(mockEnv)
        } yield run
      }
      assertM(result)(isUnit)
    },
    testM("feature branch (+minor) and master (+major) both advance version and should return version 1.0.0") {
      val mockEnv: ULayer[Console] = MockConsole.PutStrLn(equalTo("1.0.0"))
      val result = test { tempDir =>
        for {
          _ <- fix("code1.py", tempDir)
          _ <- run(tempDir, "tag")
          _ <- branch("feature/f1", tempDir)
          _ <- feat("code2.py", tempDir)
          _ <- checkout("master", tempDir)
          _ <- major("code3.py", tempDir)
          _ <- merge("feature/f1", tempDir)
          //_ <- println(ProcessUtils.exec("git log --graph --full-history --color --oneline", Some(tempDir)).stdout)
          run <- run(tempDir, "next").provideCustomLayer(mockEnv)
        } yield run
      }
      assertM(result)(isUnit)
    },
    testM("feature branch (+major) and master (+minor) both advance version and should return version 1.0.0") {
      val mockEnv: ULayer[Console] = MockConsole.PutStrLn(equalTo("1.0.0"))
      val result = test { tempDir =>
        for {
          _ <- fix("code1.py", tempDir)
          _ <- run(tempDir, "tag")
          _ <- branch("feature/f1", tempDir)
          _ <- major("code2.py", tempDir)
          _ <- checkout("master", tempDir)
          _ <- feat("code3.py", tempDir)
          _ <- merge("feature/f1", tempDir)
          //_ <- println(ProcessUtils.exec("git log --graph --full-history --color --oneline", Some(tempDir)).stdout)
          run <- run(tempDir, "next").provideCustomLayer(mockEnv)
        } yield run
      }
      assertM(result)(isUnit)
    },
    testM("feature branch 1 (+major) and feature branch 2 (+minor) both advance version and should return version 1.0.0") {
      val mockEnv: ULayer[Console] = MockConsole.PutStrLn(equalTo("1.0.0"))
      val result = test { tempDir =>
        for {
          _ <- fix("code1.py", tempDir)
          _ <- run (tempDir, "tag")

          _ <- branch ("feature/f1", tempDir)
          _ <- feat ("code2.py", tempDir)
          _ <- checkout ("master", tempDir)

          _ <- branch ("feature/f2", tempDir)
          _ <- major ("code3.py", tempDir)
          _ <- checkout ("master", tempDir)

          _ <- merge ("feature/f1", tempDir)
          _ <- merge ("feature/f2", tempDir)
          //_ <- println (ProcessUtils.exec("git log --graph --full-history --color --oneline", Some(tempDir)).stdout)
          run <- run(tempDir, "next").provideCustomLayer(mockEnv)
        } yield run
      }
      assertM(result)(isUnit)
    }
  )

  def test[R, E, A](f: File => ZIO[R, E, A]) = {
    Files.usingTempDirectory("git-mkver") { tempDir: Path =>
      init(tempDir.toFile).flatMap { _ =>
        f(tempDir.toFile)
      }
    }
  }

  def run(tempDir: File, command: String): ZIO[zio.ZEnv, Throwable, Unit] = {
    // TODO provide layer with git that has different working dir
    mainImpl(List(command)).provideCustomLayer(Blocking.live >>> Git.live(Some(tempDir)))
  }

  def init(tempDir: File): RIO[Blocking, Unit] = {
    for {
      _ <- exec(Array("git", "init"), Some(tempDir))
      _ <- exec(Array("git", "config", "user.name", "Mona Lisa"), Some(tempDir))
      _ <- exec(Array("git", "config", "user.email", "mona.lisa@email.org"), Some(tempDir))
    } yield ()
  }

  def fix(name: String, tempDir: File): RIO[Blocking, Unit] = {
    for {
      path <- Path(tempDir.file.toString, name)
      _ <- Files.touch(path)
      _ <- exec (Array("git", "add", "."), Some(tempDir))
      _ <- exec (Array("git", "commit", "-m", s"fix: $name"), Some(tempDir))
    } yield ()
  }

  def feat(name: String, tempDir: File): RIO[Blocking, Unit] = {
    for {
      path <- Path(tempDir.file.toString, name)
      _ <- Files.touch(path)
      _ <- exec(Array("git", "add", "."), Some(tempDir))
      _ <- exec(Array("git", "commit", "-m", s"feat: $name"), Some(tempDir))
    } yield ()
  }

  def major(name: String, tempDir: File): RIO[Blocking, Unit] = {
    for {
      path <- Path(tempDir.file.toString, name)
      _ <- Files.touch(path)
      _ <- exec(Array("git", "add", "."), Some(tempDir))
      _ <- exec(Array("git", "commit", "-m", s"major: $name"), Some(tempDir))
    } yield ()
  }

  def branch(name: String, tempDir: File): RIO[Blocking, Unit] = {
    exec(Array("git", "checkout", "-b", name), Some(tempDir))
  }

  def merge(name: String, tempDir: File): RIO[Blocking, Unit] = {
    exec(Array("git", "merge", "--no-ff", name), Some(tempDir))
  }

  def checkout(name: String, tempDir: File): RIO[Blocking, Unit] = {
    exec(Array("git", "checkout", name), Some(tempDir))
  }

  def exec(commands: Array[String], dir: Option[File] = None): RIO[Blocking, Unit] = {
    ProcessUtils.exec(commands, dir).flatMap { result =>
      RIO.fail(MkVerException(result.stdout)).when(result.exitCode != 0)
    }
  }
}

