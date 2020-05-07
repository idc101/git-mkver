package net.cardnell.mkver

import cats.implicits._
import com.typesafe.config.ConfigFactory
import zio.Task
import zio.config.ConfigDescriptor._
import zio.config._
import zio.config.typesafe.TypesafeConfigSource

sealed trait VersionMode

object VersionMode {
  case object SemVer extends VersionMode
  case object SemVerPreRelease extends VersionMode
  case object YearMonth extends VersionMode
  case object YearMonthPreRelease extends VersionMode

  def read(value: String): Either[String, VersionMode] =
    value match {
      case "SemVer" => Right(SemVer)
      case _ => Left("VersionMode must be one of: ")
    }
}

sealed trait WhenNoValidCommitMessages

object WhenNoValidCommitMessages {
  case object Fail extends WhenNoValidCommitMessages
  case object IncrementMajor extends WhenNoValidCommitMessages
  case object IncrementMinor extends WhenNoValidCommitMessages
  case object IncrementPatch extends WhenNoValidCommitMessages
  case object NoIncrement extends WhenNoValidCommitMessages

  def read(value: String): Either[String, WhenNoValidCommitMessages] =
    value match {
      case "Fail" => Right(Fail)
      case "IncrementMajor" => Right(IncrementMajor)
      case "IncrementMinor" => Right(IncrementMinor)
      case "IncrementPatch" => Right(IncrementPatch)
      case "NoIncrement" => Right(NoIncrement)
      case _ => Left("WhenNoValidCommitMessages should be one of Fail|IncrementMajor|IncrementMinor|IncrementPatch|NoIncrement")
    }
}

case class Format(name: String, format: String)
object Format {
  val formatDesc = (
    string("name").describe("Name of format. e.g. 'MajorMinor'") |@|
    string("format").describe("Format string for this format. Can include other formats. e.g. '{x}.{y}'")
    )(Format.apply, Format.unapply)
}

case class BranchConfig(name: String,
                        versionFormat: String,
                        tag: Boolean,
                        tagPrefix: String,
                        tagMessageFormat: String,
                        preReleaseName: String,
                        whenNoValidCommitMessages: WhenNoValidCommitMessages,
                        formats: List[Format],
                        patches: List[String])

case class BranchConfigOpt(name: String,
                           versionFormat: Option[String],
                           tag: Option[Boolean],
                           tagPrefix: Option[String],
                           tagMessageFormat: Option[String],
                           preReleaseName: Option[String],
                           whenNoValidCommitMessages: Option[WhenNoValidCommitMessages],
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
      .xmapEither(WhenNoValidCommitMessages.read, (output: WhenNoValidCommitMessages) => Right(output.toString))
      .describe("behaviour if no valid commit messages are found Fail|IncrementMajor|IncrementMinor|IncrementPatch")
    val formatsDesc = nested("formats")(list(Format.formatDesc)).describe("custom format strings")
    val patchesDesc = list("patches")(string).describe("Patch configs to be applied")

  val branchConfigDesc = (
      nameDesc.default(".*") |@|
      versionFormatDesc.default("VersionBuildMetaData") |@|
      tagDesc.default(false) |@|
      tagPrefixDesc.default("v") |@|
      tagMessageFormatDesc.default("release {Version}") |@|
      preReleaseNameDesc.default("rc") |@|
      whenNoValidCommitMessages.default(WhenNoValidCommitMessages.IncrementMinor) |@|
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

case class AppConfig(mode: VersionMode, defaults: BranchConfig, branches: List[BranchConfigOpt], patches: List[PatchConfig])

object AppConfig {
  val appConfigDesc = (
      string("mode").xmapEither(VersionMode.read, (output: VersionMode) => Right(output.toString))
        .describe("The Version Mode for this repository") |@|
      nested("defaults")(BranchConfig.branchConfigDesc) |@|
      nested("branches")(list(BranchConfig.branchConfigOptDesc).default(Nil)) |@|
      nested("patches")(list(PatchConfig.patchConfigDesc).default(Nil))
    )(AppConfig.apply, AppConfig.unapply)

  def getBranchConfig(configFile: Option[String], currentBranch: String): Task[BranchConfig] = {
    getAppConfig(configFile).flatMap { appConfig =>
      getReferenceConfig.map { refConfig =>
        val defaults = appConfig.defaults.copy(formats = mergeFormats(refConfig.defaults.formats, appConfig.defaults.formats))

        val branchConfig = appConfig.branches.find { bc => currentBranch.matches(bc.name) }

        branchConfig.map { bc =>
          BranchConfig(
            name = bc.name,
            versionFormat = bc.versionFormat.getOrElse(defaults.versionFormat),
            tag = bc.tag.getOrElse(defaults.tag),
            tagPrefix = bc.tagPrefix.getOrElse(defaults.tagPrefix),
            tagMessageFormat = bc.tagMessageFormat.getOrElse(defaults.tagMessageFormat),
            preReleaseName = bc.preReleaseName.getOrElse(defaults.preReleaseName),
            whenNoValidCommitMessages = bc.whenNoValidCommitMessages.getOrElse(defaults.whenNoValidCommitMessages),
            formats = mergeFormats(defaults.formats, bc.formats.getOrElse(Nil)),
            patches = bc.patches.getOrElse(defaults.patches)
          )
        }.getOrElse(defaults)
      }
    }
  }

  def mergeFormats(startList: List[Format], overrides: List[Format]): List[Format] = {
    val startMap = startList.map( it => (it.name, it)).toMap
    val overridesMap = overrides.map( it => (it.name, it)).toMap
    overridesMap.values.foldLeft(startMap)((a, n) => a.+((n.name, n))).values.toList.sortBy(_.name)
  }

  def getPatchConfigs(configFile: Option[String], branchConfig: BranchConfig): Task[List[PatchConfig]] = {
    getAppConfig(configFile).flatMap { appConfig =>
      val allPatchConfigs = appConfig.patches.map(it => (it.name, it)).toMap
      Task.foreach(branchConfig.patches) { c =>
        allPatchConfigs.get(c) match {
          case Some(p) => Task.succeed(p)
          case None => Task.fail(MkVerException(s"Can't find patch config named $c"))
        }
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
