package net.cardnell.mkver

import cats.implicits._
import com.typesafe.config.ConfigFactory
import zio.Task
import zio.config.ConfigDescriptor.{string, _}
import zio.config._
import zio.config.typesafe.TypesafeConfigSource


case class Format(name: String, format: String)

object Format {
  val formatDesc = (
    string("name").describe("Name of format. e.g. 'MajorMinor'") |@|
    string("format").describe("Format string for this format. Can include other formats. e.g. '{x}.{y}'")
    )(Format.apply, Format.unapply)
}

case class RunConfig(name: String,
                     versionFormat: String,
                     tag: Boolean,
                     tagPrefix: String,
                     tagMessageFormat: String,
                     preReleaseName: String,
                     commitMessageActions: List[CommitMessageAction],
                     whenNoValidCommitMessages: IncrementAction,
                     formats: List[Format],
                     patches: List[PatchConfig])

case class BranchConfig(name: String,
                        versionFormat: String,
                        tag: Boolean,
                        tagPrefix: String,
                        tagMessageFormat: String,
                        preReleaseName: String,
                        whenNoValidCommitMessages: IncrementAction,
                        formats: List[Format],
                        patches: List[String])

case class BranchConfigOpt(name: String,
                           versionFormat: Option[String],
                           tag: Option[Boolean],
                           tagPrefix: Option[String],
                           tagMessageFormat: Option[String],
                           preReleaseName: Option[String],
                           whenNoValidCommitMessages: Option[IncrementAction],
                           formats: Option[List[Format]],
                           patches: Option[List[String]])

object BranchConfig {
    val nameDesc = string("name").describe("regex to match branch name on")
    val versionFormatDesc = string("versionFormat").describe("the parts of the version number to be used")
    val tagDesc = boolean("tag").describe("whether to actually tag this branch when `mkver tag` is called")
    val tagPrefixDesc = string("tagPrefix").describe("prefix for git tags")
    val tagMessageFormatDesc = string("tagMessageFormat").describe("A format to be used in the annotated git tag message")
    val preReleaseNameDesc = string("preReleaseName").describe("name of the pre-release. e.g. alpha, beta, rc")
    val whenNoValidCommitMessages = string("whenNoValidCommitMessages")
      .xmapEither(IncrementAction.read, (output: IncrementAction) => Right(output.toString))
      .describe("behaviour if no valid commit messages are found Fail|IncrementMajor|IncrementMinor|IncrementPatch|NoIncrement")
    val formatsDesc = nested("formats")(list(Format.formatDesc)).describe("custom format strings")
    val patchesDesc = list("patches")(string).describe("Patch configs to be applied")

  val branchConfigDesc = (
      nameDesc.default(".*") |@|
      versionFormatDesc.default("VersionBuildMetaData") |@|
      tagDesc.default(false) |@|
      tagPrefixDesc.default("v") |@|
      tagMessageFormatDesc.default("release {Version}") |@|
      preReleaseNameDesc.default("rc") |@|
      whenNoValidCommitMessages.default(IncrementAction.IncrementMinor) |@|
      formatsDesc.default(Nil) |@|
      patchesDesc.default(Nil)
    )(BranchConfig.apply, BranchConfig.unapply)

  val branchConfigOptDesc = (
      nameDesc |@|
      versionFormatDesc.optional |@|
      tagDesc.optional |@|
      tagPrefixDesc.optional |@|
      tagMessageFormatDesc.optional |@|
      preReleaseNameDesc.optional |@|
      whenNoValidCommitMessages.optional |@|
      formatsDesc.optional |@|
      patchesDesc.optional
    )(BranchConfigOpt.apply, BranchConfigOpt.unapply)
}

case class PatchConfig(name: String, filePatterns: List[String], find: String, replace: String)

object PatchConfig {
  val patchConfigDesc = (
      string("name").describe("Name of patch, referenced from branch configs") |@|
      list("filePatterns")(string).describe("Files to apply find and replace in. Supports ** and * glob patterns.") |@|
      string("find").describe("Regex to find in file") |@|
      string("replace").describe("Replacement string. Can include version format strings (see help)")
    )(PatchConfig.apply, PatchConfig.unapply)
}

case class CommitMessageAction(pattern: String, action: IncrementAction)

object CommitMessageAction {
  val commitMessageActionDesc = (
    string("pattern").describe("Regular expression to match a commit message line") |@|
    string("action")
      .xmapEither(IncrementAction.read, (output: IncrementAction) => Right(output.toString))
      .describe("Version Increment behaviour if a commit line matches the regex Fail|IncrementMajor|IncrementMinor|IncrementPatch|NoIncrement")
    )(CommitMessageAction.apply, CommitMessageAction.unapply)
}

case class AppConfig(mode: VersionMode,
                     defaults: BranchConfig,
                     branches: List[BranchConfigOpt],
                     patches: List[PatchConfig],
                     commitMessageActions: List[CommitMessageAction])

object AppConfig {
  val appConfigDesc = (
      string("mode").xmapEither(VersionMode.read, (output: VersionMode) => Right(output.toString))
        .describe("The Version Mode for this repository") |@|
      nested("defaults")(BranchConfig.branchConfigDesc) |@|
      nested("branches")(list(BranchConfig.branchConfigOptDesc).default(Nil)) |@|
      nested("patches")(list(PatchConfig.patchConfigDesc).default(Nil)) |@|
      nested("commitMessageActions")(list(CommitMessageAction.commitMessageActionDesc).default(Nil))
    )(AppConfig.apply, AppConfig.unapply)

  def getRunConfig(configFile: Option[String], currentBranch: String): Task[RunConfig] = {
    for {
      appConfig <- getAppConfig(configFile)
      refConfig <- getReferenceConfig
      defaults = appConfig.defaults.copy(formats = mergeFormats(refConfig.defaults.formats, appConfig.defaults.formats))
      branchConfig = appConfig.branches.find { bc => currentBranch.matches(bc.name) }
      patchNames = branchConfig.flatMap(_.patches).getOrElse(defaults.patches)
      patchConfigs <- getPatchConfigs(appConfig, patchNames)
    } yield {
      branchConfig.map { bc =>
        RunConfig(
          name = bc.name,
          versionFormat = bc.versionFormat.getOrElse(defaults.versionFormat),
          tag = bc.tag.getOrElse(defaults.tag),
          tagPrefix = bc.tagPrefix.getOrElse(defaults.tagPrefix),
          tagMessageFormat = bc.tagMessageFormat.getOrElse(defaults.tagMessageFormat),
          preReleaseName = bc.preReleaseName.getOrElse(defaults.preReleaseName),
          commitMessageActions = mergeCommitMessageActions(refConfig.commitMessageActions, appConfig.commitMessageActions),
          whenNoValidCommitMessages = bc.whenNoValidCommitMessages.getOrElse(defaults.whenNoValidCommitMessages),
          formats = mergeFormats(defaults.formats, bc.formats.getOrElse(Nil)),
          patches = patchConfigs
        )
      }.getOrElse {
        RunConfig(
          name = defaults.name,
          versionFormat = defaults.versionFormat,
          tag = defaults.tag,
          tagPrefix = defaults.tagPrefix,
          tagMessageFormat = defaults.tagMessageFormat,
          preReleaseName = defaults.preReleaseName,
          commitMessageActions = appConfig.commitMessageActions,
          whenNoValidCommitMessages = defaults.whenNoValidCommitMessages,
          formats = defaults.formats,
          patches = patchConfigs
        )
      }
    }
  }

  def mergeFormats(startList: List[Format], overrides: List[Format]): List[Format] =
    merge(startList, overrides, (f: Format) => f.name)

  def mergeCommitMessageActions(startList: List[CommitMessageAction], overrides: List[CommitMessageAction]): List[CommitMessageAction] =
    merge(startList, overrides, (cma: CommitMessageAction) => cma.pattern)

  def merge[T](startList: List[T], overrides: List[T], getName: T => String): List[T] = {
    val startMap = startList.map( it => (getName(it), it)).toMap
    val overridesMap = overrides.map( it => (getName(it), it)).toMap
    overridesMap.values.foldLeft(startMap)((a, n) => a.+((getName(n), n))).values.toList.sortBy(getName(_))
  }

  def getPatchConfigs(appConfig: AppConfig, patchNames: List[String]): Task[List[PatchConfig]] = {
    val allPatchConfigs = appConfig.patches.map(it => (it.name, it)).toMap
    Task.foreach(patchNames) { c =>
      allPatchConfigs.get(c) match {
        case Some(p) => Task.succeed(p)
        case None => Task.fail(MkVerException(s"Can't find patch config named $c"))
      }
    }
  }

  def getAppConfig(configFile: Option[String]): Task[AppConfig] = {
    val file = configFile.map { cf =>
      for {
        path <- Path(cf)
        exists <- Files.exists(path)
        r <- if (exists) Task.some(cf) else Task.fail(MkVerException(s"--config $cf does not exist"))
      } yield r
    }.orElse {
      sys.env.get("GITMKVER_CONFIG").map { cf =>
        for {
          path <- Path(cf)
          exists <- Files.exists(path)
          r <- if (exists) Task.some(cf) else Task.fail(MkVerException(s"GITMKVER_CONFIG $cf does not exist"))
        } yield r
      }
    }.getOrElse {
      for {
        path <- Path("mkver.conf")
        exists <- Files.exists(path)
        r <- if (exists) Task.some("mkver.conf") else Task.none
      } yield r
    }

    file.flatMap { of =>
      of.map { f =>
        TypesafeConfigSource.fromTypesafeConfig(ConfigFactory.parseFile(new java.io.File(f)))
        // TODO Use this?
        //TypesafeConfig.fromHoconFile(new java.io.File(c), AppConfig.appConfigDesc)
      }.getOrElse {
        TypesafeConfigSource.fromTypesafeConfig(ConfigFactory.load("reference.conf"))
      }.fold(l => Task.fail(MkVerException(l)), r => Task.succeed(r))
    }.flatMap { source: ConfigSource[String, String] =>
      read(AppConfig.appConfigDesc from source) match {
        case Left(value) => Task.fail(MkVerException("Unable to parse config: " + value))
        case Right(result) => Task.succeed(result)
      }
    }
  }

  def getReferenceConfig: Task[AppConfig] = {
    val source = TypesafeConfigSource.fromTypesafeConfig(ConfigFactory.load("reference.conf"))
    source.fold(l => Task.fail(MkVerException(l)), r => Task.succeed(r))
      .flatMap { source: ConfigSource[String, String] =>
      read(AppConfig.appConfigDesc from source) match {
        case Left(value) => Task.fail(MkVerException("Unable to parse config: " + value))
        case Right(result) => Task.succeed(result)
      }
    }
  }
}
