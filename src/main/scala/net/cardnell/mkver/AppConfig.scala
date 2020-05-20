package net.cardnell.mkver

import cats.implicits._
import com.typesafe.config.ConfigFactory
import net.cardnell.mkver.IncrementAction.IncrementMinor
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

case class RunConfig(versionFormat: String,
                     tag: Boolean,
                     tagPrefix: String,
                     tagMessageFormat: String,
                     preReleaseName: String,
                     commitMessageActions: List[CommitMessageAction],
                     whenNoValidCommitMessages: IncrementAction,
                     formats: List[Format],
                     patches: List[PatchConfig])

case class BranchConfigDefaults(versionFormat: String,
                                tag: Boolean,
                                tagMessageFormat: String,
                                preReleaseName: String,
                                whenNoValidCommitMessages: IncrementAction,
                                formats: List[Format],
                                patches: List[String])

case class BranchConfig(pattern: String,
                        versionFormat: Option[String],
                        tag: Option[Boolean],
                        tagMessageFormat: Option[String],
                        preReleaseName: Option[String],
                        whenNoValidCommitMessages: Option[IncrementAction],
                        formats: Option[List[Format]],
                        patches: Option[List[String]])

object BranchConfig {
    val patternDesc = string("pattern").describe("regex to match branch name on")
    val versionFormatDesc = string("versionFormat").describe("the parts of the version number to be used")
    val tagDesc = boolean("tag").describe("whether to actually tag this branch when `mkver tag` is called")
    val tagMessageFormatDesc = string("tagMessageFormat").describe("A format to be used in the annotated git tag message")
    val preReleaseNameDesc = string("preReleaseName").describe("name of the pre-release. e.g. alpha, beta, rc")
    val whenNoValidCommitMessages = string("whenNoValidCommitMessages")
      .xmapEither(IncrementAction.read, (output: IncrementAction) => Right(output.toString))
      .describe("behaviour if no valid commit messages are found Fail|IncrementMajor|IncrementMinor|IncrementPatch|NoIncrement")
    val formatsDesc = nested("formats")(list(Format.formatDesc)).describe("custom format strings")
    val patchesDesc = list("patches")(string).describe("Patch configs to be applied")

  val branchConfigDefaultsDesc = (
      versionFormatDesc.default("VersionBuildMetaData") |@|
      tagDesc.default(false) |@|
      tagMessageFormatDesc.default("release {Version}") |@|
      preReleaseNameDesc.default("rc") |@|
      whenNoValidCommitMessages.default(IncrementAction.IncrementMinor) |@|
      formatsDesc.default(Nil) |@|
      patchesDesc.default(Nil)
    )(BranchConfigDefaults.apply, BranchConfigDefaults.unapply)

  val branchConfigDesc = (
      patternDesc |@|
      versionFormatDesc.optional |@|
      tagDesc.optional |@|
      tagMessageFormatDesc.optional |@|
      preReleaseNameDesc.optional |@|
      whenNoValidCommitMessages.optional |@|
      formatsDesc.optional |@|
      patchesDesc.optional
    )(BranchConfig.apply, BranchConfig.unapply)
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
                     tagPrefix: Option[String],
                     defaults: Option[BranchConfigDefaults],
                     branches: Option[List[BranchConfig]],
                     patches: Option[List[PatchConfig]],
                     commitMessageActions: Option[List[CommitMessageAction]])

object AppConfig {
  val appConfigDesc = (
      string("mode").xmapEither(VersionMode.read, (output: VersionMode) => Right(output.toString))
        .describe("The Version Mode for this repository")
        .default(VersionMode.SemVer) |@|
      string("tagPrefix").describe("prefix for git tags").optional |@|
      nested("defaults")(BranchConfig.branchConfigDefaultsDesc).optional |@|
      nested("branches")(list(BranchConfig.branchConfigDesc)).optional |@|
      nested("patches")(list(PatchConfig.patchConfigDesc)).optional |@|
      nested("commitMessageActions")(list(CommitMessageAction.commitMessageActionDesc)).optional
    )(AppConfig.apply, AppConfig.unapply)

  object Defaults {
    val name = ".*"
    val versionFormat = "VersionBuildMetaData"
    val tag = false
    val tagMessageFormat = "release {Tag}"
    val preReleaseName = "rc"
    val whenNoValidCommitMessages = IncrementMinor
    val patches = Nil
    val formats = Nil
  }
  val defaultDefaultBranchConfig: BranchConfigDefaults = BranchConfigDefaults(
    Defaults.versionFormat,
    Defaults.tag,
    Defaults.tagMessageFormat,
    Defaults.preReleaseName,
    Defaults.whenNoValidCommitMessages,
    Defaults.patches,
    Defaults.formats
  )
  val defaultBranchConfigs: List[BranchConfig] = List(
    BranchConfig("master", Some("Version"), Some(true), None, None, None, None, None)
  )
  val defaultPatchConfigs: List[PatchConfig] = Nil
  val defaultCommitMessageActions: List[CommitMessageAction] = List(
    CommitMessageAction("BREAKING CHANGE", IncrementAction.IncrementMajor),
    CommitMessageAction("major(\\(.+\\))?:", IncrementAction.IncrementMajor),
    CommitMessageAction("minor(\\(.+\\))?:", IncrementAction.IncrementMinor),
    CommitMessageAction("patch(\\(.+\\))?:", IncrementAction.IncrementPatch),
    CommitMessageAction("feat(\\(.+\\))?:", IncrementAction.IncrementMinor),
    CommitMessageAction("fix(\\(.+\\))?:", IncrementAction.IncrementPatch)
  )
  val defaultFormats: List[Format] = List(Format("BuildMetaData", "{Branch}.{ShortHash}"))

  def getRunConfig(configFile: Option[String], currentBranch: String): Task[RunConfig] = {
    for {
      appConfig <- getAppConfig(configFile)
      defaults = appConfig.defaults.getOrElse(defaultDefaultBranchConfig)
        .copy(formats = mergeFormats(defaultDefaultBranchConfig.formats, appConfig.defaults.map(_.formats).getOrElse(Nil)))
      branchConfig = appConfig.branches.getOrElse(defaultBranchConfigs)
        .find { bc => currentBranch.matches(bc.pattern) }
      patchNames = branchConfig.map(_.patches.getOrElse(Nil)).getOrElse(defaults.patches)
      patchConfigs <- getPatchConfigs(appConfig, patchNames)
    } yield {
      RunConfig(
        versionFormat = branchConfig.flatMap(_.versionFormat).getOrElse(defaults.versionFormat),
        tag = branchConfig.flatMap(_.tag).getOrElse(defaults.tag),
        tagPrefix = appConfig.tagPrefix.getOrElse("v"),
        tagMessageFormat = branchConfig.flatMap(_.tagMessageFormat).getOrElse(defaults.tagMessageFormat),
        preReleaseName = branchConfig.flatMap(_.preReleaseName).getOrElse(defaults.preReleaseName),
        commitMessageActions = mergeCommitMessageActions(defaultCommitMessageActions, appConfig.commitMessageActions.getOrElse(Nil)),
        whenNoValidCommitMessages = branchConfig.flatMap(_.whenNoValidCommitMessages).getOrElse(defaults.whenNoValidCommitMessages),
        formats = mergeFormats(defaultFormats, branchConfig.flatMap(_.formats).getOrElse(Nil)),
        patches = patchConfigs
      )
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
    val allPatchConfigs = appConfig.patches.getOrElse(Nil).map(it => (it.name, it)).toMap
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
    }.flatMap { source: ConfigSource =>
      read(AppConfig.appConfigDesc from source) match {
        case Left(value) => Task.fail(MkVerException("Unable to parse config: " + value))
        case Right(result) => Task.succeed(result)
      }
    }
  }
}
