package net.cardnell.mkver

import scala.collection.JavaConverters._
import java.nio.file.{Files => JFiles, Path => JPath, Paths => JPaths}
import java.io.{File => JFile}

import zio.blocking.Blocking
import zio.{RIO, Task, URIO, ZIO}
import zio.stream.Stream

object Files {
  def exists(path: Path): Task[Boolean] = {
    ZIO.effect(JFiles.exists(path.path))
  }

  def glob(path: Path, glob: String): ZIO[Any, Throwable, Stream[Nothing, Path]] = {
     ZIO.effect(JFiles.newDirectoryStream(path.path, glob)).map {ds =>
      Stream.fromIterable(ds.asScala).map(Path(_))
    }
  }

  val currentWorkingDirectory = Path(new JFile("").toString)

  def readAllLines(path: Path): Task[List[String]] = {
    ZIO.effect(JFiles.readAllLines(path.path).asScala.toList)
  }

  def write(path: Path, lines: Iterable[String]) = {
    ZIO.effect(JFiles.write(path.path, lines.asJava))
  }

  def createTempDirectory(prefix: String) = {
    ZIO.effect(JFiles.createTempDirectory(prefix)).map(Path(_))
  }

  def usingTempDirectory[R, E, A](prefix: String)(f: Path => ZIO[R, E, A]) = {
    createTempDirectory("git-mkver")
      .bracket((p: Path) => ZIO.effect({ p.path.toFile.delete() }).ignore) { tempDir: Path =>
        f(tempDir)
      }
  }

  def touch(path: Path) = {
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
  def toFile() = File(path.toFile)
}

object Path {
  def apply(first: String, more: String*): ZIO[Any, Throwable, Path] = {
    ZIO.effect(JPaths.get(first, more:_*)).map(p => Path(p))
  }
}
