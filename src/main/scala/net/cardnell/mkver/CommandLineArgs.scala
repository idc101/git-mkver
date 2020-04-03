package net.cardnell.mkver

import com.monovore.decline.{Command, Opts}
import cats.implicits._

object CommandLineArgs {
  case class NextOpts(format: Option[String])
  case class TagOpts(format: Option[String])
  case class PatchOpts(format: Option[String])

  val configFile: Opts[Option[String]] = Opts.option[String]("config", short = "c", metavar = "file", help = "Config file to load").orNone
  val format: Opts[Option[String]] = Opts.option[String]("format", short = "f", metavar = "string", help = "Format string for the version number").orNone
  //val port = Opts.env[Int]("PORT", help = "The port to run on.")
  val nextOptions: Opts[NextOpts] = format.map(NextOpts.apply)
  val tagOptions: Opts[TagOpts] = format.map(TagOpts.apply)
  val patchOptions: Opts[PatchOpts] = format.map(PatchOpts.apply)

  val nextCommand: Command[NextOpts] = Command("next", header = "Print the next version tag that would be used") {
    nextOptions
  }

  val tagCommand: Command[TagOpts] = Command("tag", header = "Git Tag the current commit with the next version tag") {
    tagOptions
  }

  val patchCommand: Command[PatchOpts] = Command("patch", header = "Patch version information in files with the next version tag") {
    patchOptions
  }

  case class CommandLineOpts(configFile: Option[String], p: Product)

  val commands: Opts[Product] = Opts.subcommands(nextCommand, tagCommand, patchCommand)

  val commandLineOpts: Opts[CommandLineOpts] = (configFile, commands).mapN(CommandLineOpts.apply)

  val mkverCommand: Command[CommandLineOpts] = Command(
    name = "git-mkver",
    header = "Uses git tags, branch names and commit messages to determine the next version of the software to release"
  ) {
    commandLineOpts
  }
}
