package net.cardnell.mkver

import java.io.{File => JFile}
import java.nio.file.{FileSystems, Files => JFiles, Path => JPath, Paths => JPaths}

import zio.{Task, URIO, ZIO}

import scala.collection.JavaConverters._

object Files {
  def exists(path: Path): Task[Boolean] = {
    ZIO.effect(JFiles.exists(path.path))
  }

  def glob(path: Path, glob: String): ZIO[Any, Throwable, List[Path]] = {
    for {
      fs <- ZIO.effect(FileSystems.getDefault)
      matcher <- ZIO.effect(fs.getPathMatcher(s"glob:$glob"))
      list <- ZIO.effect(JFiles.walk(path.path).iterator().asScala.filter(matcher.matches).map(Path(_)).toList)
    } yield list
  }

  def readAllLines(path: Path): Task[List[String]] = {
    ZIO.effect(JFiles.readAllLines(path.path).asScala.toList)
  }

  def readAll(path: Path): Task[String] = {
    ZIO.effect(scala.io.Source.fromFile(path.path.toString)).bracket(s => ZIO.effect(s.close()).ignore, { s =>
      ZIO.effect(s.mkString)
    })
  }

  def write(path: Path, lines: Iterable[String]): Task[JPath] = {
    ZIO.effect(JFiles.write(path.path, lines.asJava))
  }

  def write(path: Path, content: String): Task[JPath] = {
    ZIO.effect(JFiles.write(path.path, content.getBytes))
  }

  def createTempDirectory(prefix: String): ZIO[Any, Throwable, Path] = {
    ZIO.effect(JFiles.createTempDirectory(prefix)).map(Path(_))
  }

  def usingTempDirectory[R, E, A](prefix: String)(f: Path => ZIO[R, E, A]): ZIO[R, Any, A] = {
    createTempDirectory("git-mkver")
      .bracket((p: Path) => ZIO.effect({ p.path.toFile.delete() }).ignore) { tempDir: Path =>
        f(tempDir)
      }
  }

  def touch(path: Path): URIO[Any, Unit] = {
    ZIO.effect(JFiles.createFile(path.path)).ignore
  }
}

case class File(file: JFile) {

}

object File {
  def apply(path: String): ZIO[Any, Throwable, File] = {
    ZIO.effect(new JFile(path)).map(f => File(f))
  }
}

case class Path(path: JPath) {
  def toFile: File = File(path.toFile)
}

object Path {
  def apply(first: String, more: String*): ZIO[Any, Throwable, Path] = {
    ZIO.effect(JPaths.get(first, more:_*)).map(p => Path(p))
  }

  val currentWorkingDirectory: ZIO[Any, Throwable, Path] = apply("")
}
