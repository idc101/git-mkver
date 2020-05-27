package net.cardnell.mkver

import com.monovore.decline.{Command, Opts}
import cats.implicits._

object CommandLineArgs {
  case class NextOpts(format: Option[String], preRelease: Boolean, prefix: Boolean)
  case class TagOpts(preRelease: Boolean)
  case class PatchOpts(preRelease: Boolean)
  case class InfoOpts(preRelease: Boolean, includeBranchConfig: Boolean)

  val configFile: Opts[Option[String]] = Opts.option[String]("config", short = "c", metavar = "file", help = "Config file to load").orNone
  val format: Opts[Option[String]] = Opts.option[String]("format", short = "f", metavar = "string", help = "Format string for the version number").orNone
  val prefix: Opts[Boolean] = Opts.flag("tag-prefix", short = "t", help = "Include the tag prefix in the output").orFalse
  val preRelease: Opts[Boolean] = Opts.flag("pre-release", short = "p", help = "Include the tag prefix in the output").orFalse
  val includeBranchConfig: Opts[Boolean] = Opts.flag("include-branch-config", short = "i", help = "Format string for the version number").orFalse

  val nextOptions: Opts[NextOpts] = (format, preRelease, prefix).mapN(NextOpts.apply)
  val tagOptions: Opts[TagOpts] = preRelease.map(TagOpts.apply)
  val patchOptions: Opts[PatchOpts] = preRelease.map(PatchOpts.apply)
  val infoOptions: Opts[InfoOpts] = (preRelease, includeBranchConfig).mapN(InfoOpts.apply)

  val nextCommand: Command[NextOpts] = Command("next", header = "Print the next version tag that would be used") {
    nextOptions
  }

  val tagCommand: Command[TagOpts] = Command("tag", header = "Git Tag the current commit with the next version tag") {
    tagOptions
  }

  val patchCommand: Command[PatchOpts] = Command("patch", header = "Patch version information in files with the next version tag") {
    patchOptions
  }

  val infoCommand: Command[InfoOpts] = Command("info", header = "output all formats and branch configuration") {
    infoOptions
  }

  case class CommandLineOpts(configFile: Option[String], p: Product)

  val commands: Opts[Product] = Opts.subcommands(nextCommand, tagCommand, patchCommand, infoCommand)

  val commandLineOpts: Opts[CommandLineOpts] = (configFile, commands).mapN(CommandLineOpts.apply)

  val mkverCommand: Command[CommandLineOpts] = Command(
    name = s"git-mkver - v${GitMkverVersion}",
    header = "Uses git tags, branch names and commit messages to determine the next version of the software to release"
  ) {
    commandLineOpts
  }
}
