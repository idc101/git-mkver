package net.cardnell.mkver

import zio.IO
import zio.config._
import ConfigDescriptor._
import zio.config.PropertyTree._
import zio.config.ConfigDocs._
import ConfigDocs.Details._
import better.files.File
import com.typesafe.config.ConfigFactory
import zio.config.typesafe.{TypeSafeConfigSource, TypesafeConfig}

case class Format(name: String, format: String)
object Format {
  val formatDesc = (
    string("name").describe("Name of format. e.g. 'MajorMinor'") |@|
    string("format").describe("Format string for this format. Can include other formats. e.g. '{x}.{y}'")
    )(Format.apply, Format.unapply)
}

case class BranchConfig(name: String,
                        prefix: String,
                        tag: Boolean,
                        tagFormat: String,
                        tagMessageFormat: String,
                        preReleaseName: String,
                        formats: List[Format],
                        patches: List[String])

case class BranchConfigOpt(name: String,
                           prefix: Option[String],
                           tag: Option[Boolean],
                           tagFormat: Option[String],
                           tagMessageFormat: Option[String],
                           preReleaseName: Option[String],
                           formats: Option[List[Format]],
                           patches: Option[List[String]])

object BranchConfig {
    val nameDesc = string("name").describe("regex to match branch name on")
    val prefixDesc = string("prefix").describe("prefix for git tags")
    val tagDesc = boolean("tag").describe("whether to actually tag this branch when `mkver tag` is called")
    val tagFormatDesc = string("tagFormat").describe("")
    val tagMessageFormatDesc = string("tagMessageFormat").describe("")
    val preReleaseNameDesc = string("preReleaseName").describe("")
    val formatsDesc = nested("formats")(list(Format.formatDesc)).describe("custom format strings")
    val patchesDesc = list(string("patches")).describe("Patch configs to be applied")

  val branchConfigDesc = (
      nameDesc.default(".*") |@|
      prefixDesc.default("v") |@|
      tagDesc.default(false) |@|
      tagFormatDesc.default("version") |@|
      tagMessageFormatDesc.default("release {Version}") |@|
      preReleaseNameDesc.default("rc.") |@|
      formatsDesc.default(Nil) |@|
      patchesDesc.default(Nil)
    )(BranchConfig.apply, BranchConfig.unapply)

  val branchConfigOptDesc = (
      nameDesc |@|
      prefixDesc.optional |@|
      tagDesc.optional |@|
      tagFormatDesc.optional |@|
      tagMessageFormatDesc.optional |@|
      preReleaseNameDesc.optional |@|
      formatsDesc.optional |@|
      patchesDesc.optional
    )(BranchConfigOpt.apply, BranchConfigOpt.unapply)
}

case class PatchConfig(name: String, filePatterns: List[String], find: String, replace: String)

object PatchConfig {
  val patchConfigDesc = (
      string("name").describe("Name of patch, referenced from branch configs") |@|
      list(string("filePatterns").describe("Files to apply find and replace in. Supports ** and * glob patterns.")) |@|
      string("find").describe("Regex to find in file") |@|
      string("replace").describe("Replacement string. Can include version format strings (see help)")
    )(PatchConfig.apply, PatchConfig.unapply)
}

case class AppConfig(defaults: BranchConfig, branches: List[BranchConfigOpt], patches: List[PatchConfig])

object AppConfig {
  val appConfigDesc = (
      nested("defaults")(BranchConfig.branchConfigDesc) |@|
      nested("branches")(list(BranchConfig.branchConfigOptDesc)) |@|
      nested("patches")(list(PatchConfig.patchConfigDesc))
    )(AppConfig.apply, AppConfig.unapply)

  def getBranchConfig(configFile: Option[String], currentBranch: String): BranchConfig = {
    val appConfig = getAppConfig(configFile)
    val defaults = appConfig.defaults

    val branchConfig = appConfig.branches.find { bc => currentBranch.matches(bc.name) }

    branchConfig.map { bc =>
      BranchConfig(
        name = bc.name,
        prefix = bc.prefix.getOrElse(defaults.prefix),
        tag = bc.tag.getOrElse(defaults.tag),
        tagFormat = bc.tagFormat.getOrElse(defaults.tagFormat),
        tagMessageFormat = bc.tagMessageFormat.getOrElse(defaults.tagMessageFormat),
        preReleaseName = bc.preReleaseName.getOrElse(defaults.preReleaseName),
        formats = mergeFormats(bc.formats.getOrElse(Nil), defaults.formats),
        patches = bc.patches.getOrElse(defaults.patches)
      )
    }.getOrElse(defaults)
  }

  def mergeFormats(branch: List[Format], defaults: List[Format]): List[Format] = {
    def update(startList: List[Format], overrides: List[Format]): List[Format] = {
      val startMap = startList.map( it => (it.name, it)).toMap
      val overridesMap = overrides.map( it => (it.name, it)).toMap
      overridesMap.values.foldLeft(startMap)((a, n) => a.+((n.name, n))).values.toList.sortBy(_.name)
    }
    // Start with defaults
    val v1 = update(defaults, branch)
    val v2 = update(v1, Formatter.builtInFormats)
    v2
  }

  def getPatchConfigs(configFile: Option[String], branchConfig: BranchConfig): List[PatchConfig] = {
    val allPatchConfigs = getAppConfig(configFile).patches.map(it => (it.name, it)).toMap
    branchConfig.patches.map( c => allPatchConfigs.get(c).orElse(sys.error(s"Can't find patch config named $c")).get)
  }

  def getAppConfig(configFile: Option[String]): AppConfig = {
    val file = if (configFile.exists(File(_).exists)) {
      configFile
    } else if (sys.env.get("GITMKVER_CONFIG").exists(File(_).exists)) {
      sys.env.get("GITMKVER_CONFIG")
    } else if (File("mkver.conf").exists) {
      Some("mkver.conf")
    } else {
      None
    }

    val hocon = file.map { f =>
      TypeSafeConfigSource.fromTypesafeConfig(ConfigFactory.parseFile(new java.io.File(f)))
      // TODO Use this when in ZIO land
      // TypeSafeConfigSource.fromHoconFile(new java.io.File("mkver.conf"))
    }.getOrElse {
      TypeSafeConfigSource.fromTypesafeConfig(ConfigFactory.load("reference.conf"))
    }

    val config =
      hocon match {
        case Left(value) => sys.error("Unable to load config: " + value)
        case Right(source) => read(AppConfig.appConfigDesc from source)
      }

    config match {
      case Left(value) => sys.error("Unable to parse config: " + value)
      case Right(result) => result
    }
  }
}
