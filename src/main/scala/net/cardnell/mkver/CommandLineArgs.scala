package net.cardnell.mkver

import com.monovore.decline.{Command, Opts}

object CommandLineArgs {
  case class NextOpts(format: Option[String])
  case class TagOpts(format: Option[String])
  case class PatchOpts(format: Option[String])
  case class CiOpts(format: Option[String])

  val format: Opts[Option[String]] = Opts.option[String]("format", short = "f", metavar = "string", help = "Format string for the version number").orNone
  //val port = Opts.env[Int]("PORT", help = "The port to run on.")
  val nextOptions: Opts[NextOpts] = format.map(NextOpts.apply)
  val tagOptions: Opts[TagOpts] = format.map(TagOpts.apply)
  val patchOptions: Opts[PatchOpts] = format.map(PatchOpts.apply)
  val ciOptions: Opts[CiOpts] = format.map(CiOpts.apply)

  val nextCommand: Command[NextOpts] = Command("next", header = "Print the next version tag that would be used") {
    nextOptions
  }

  val tagCommand: Command[TagOpts] = Command("tag", header = "Git Tag the current commit with the next version tag") {
    tagOptions
  }

  val patchCommand: Command[PatchOpts] = Command("patch", header = "Patch version information in files with the next version tag") {
    patchOptions
  }

  val ciCommand: Command[CiOpts] = Command("ci", header = "Patch and then Tag as would be commonly done from a CI server") {
    ciOptions
  }

  val commands: Opts[Product] = Opts.subcommands(nextCommand, tagCommand, patchCommand, ciCommand)

  val mkverCommand: Command[Product] = Command(
    name = "git-mkver",
    header = "Uses git tags, branch names and commit messages to determine the next version of the software to release"
  ) {
    commands
  }

}
