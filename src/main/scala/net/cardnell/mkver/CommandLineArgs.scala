package net.cardnell.mkver

import com.monovore.decline.{Command, Opts}
import cats.implicits._
import cats.instances.unit
import net.cardnell.mkver.CommandLineArgs.ConfigOpts

object CommandLineArgs {
  sealed trait AppOpts
  case class NextOpts(format: Option[String], preRelease: Boolean, prefix: Boolean) extends AppOpts
  case class TagOpts(preRelease: Boolean) extends AppOpts
  case class PatchOpts(preRelease: Boolean) extends AppOpts
  case class InfoOpts(preRelease: Boolean, includeEnv: Boolean) extends AppOpts
  case object ConfigOpts extends AppOpts

  val configFile: Opts[Option[String]] = Opts.option[String]("config", short = "c", metavar = "file", help = "Config file to load").orNone
  val format: Opts[Option[String]] = Opts.option[String]("format", short = "f", metavar = "string", help = "Format string for the version number").orNone
  val prefix: Opts[Boolean] = Opts.flag("tag-prefix", short = "t", help = "Include the tag prefix in the output").orFalse
  val preRelease: Opts[Boolean] = Opts.flag("pre-release", short = "p", help = "Include the tag prefix in the output").orFalse
  val includeEnv: Opts[Boolean] = Opts.flag("include-env", short = "i", help = "Include environment variables").orFalse

  val nextOptions: Opts[NextOpts] = (format, preRelease, prefix).mapN(NextOpts.apply)
  val tagOptions: Opts[TagOpts] = preRelease.map(TagOpts.apply)
  val patchOptions: Opts[PatchOpts] = preRelease.map(PatchOpts.apply)
  val infoOptions: Opts[InfoOpts] = (preRelease, includeEnv).mapN(InfoOpts.apply)

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

  val configCommand: Command[AppOpts] = Command("config", header = "output final configuration to be used") { Opts(ConfigOpts) }

  case class CommandLineOpts(configFile: Option[String], opts: AppOpts)

  val commands: Opts[AppOpts] = Opts.subcommands(nextCommand, tagCommand, patchCommand, infoCommand, configCommand)

  val commandLineOpts: Opts[CommandLineOpts] = (configFile, commands).mapN(CommandLineOpts.apply)

  val mkverCommand: Command[CommandLineOpts] = Command(
    name = s"git mkver",
    header = s"git-mkver - v${GitMkverVersion}\n\nUses git tags, branch names and commit messages to determine the next version of the software to release"
  ) {
    commandLineOpts
  }
}
