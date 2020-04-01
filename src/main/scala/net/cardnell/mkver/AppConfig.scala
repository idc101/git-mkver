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


case class BranchConfig(name: String,
                        prefix: String,
                        tag: Boolean,
                        tagParts: TagParts,
                        tagMessageFormat: String,
                        preReleaseName: String,
                        buildMetadataFormat: String,
                        patches: List[String])

case class BranchConfigOpt(name: String,
                        prefix: Option[String],
                        tag: Option[Boolean],
                        tagParts: Option[TagParts],
                        tagMessageFormat: Option[String],
                        preReleaseName: Option[String],
                        buildMetadataFormat: Option[String],
                        patches: Option[List[String]])

object BranchConfig {
    val nameDesc = string("name").describe("regex to match branch name on")
    val prefixDesc = string("prefix").describe("prefix for git tags")
    val tagDesc = boolean("tag").describe("whether to actually tag this branch when `mkver tag` is called")
    val tagPartsDesc = string("tagParts")(TagParts.apply, TagParts.unapply).describe("")
    val tagMessageFormatDesc = string("tagMessageFormat").describe("")
    val preReleaseNameDesc = string("preReleaseName").describe("")
    val buildMetadataFormatDesc = string("buildMetadataFormat").describe("format string to produce build metadata part of a semantic version")
    val patchesDesc = list(string("patches")).describe("Patch configs to be applied")

  val branchConfigDesc = (
      nameDesc.default(".*") |@|
      prefixDesc.default("v") |@|
      tagDesc.default(false) |@|
      tagPartsDesc.default(TagParts.VersionBuildMetadata) |@|
      tagMessageFormatDesc.default("release %ver") |@|
      preReleaseNameDesc.default("rc.") |@|
      buildMetadataFormatDesc.default("%br.%sh") |@|
      patchesDesc.default(Nil)
    )(BranchConfig.apply, BranchConfig.unapply)

  val branchConfigOptDesc = (
      nameDesc |@|
      prefixDesc.optional |@|
      tagDesc.optional |@|
      tagPartsDesc.optional |@|
      tagMessageFormatDesc.optional |@|
      preReleaseNameDesc.optional |@|
      buildMetadataFormatDesc.optional |@|
      patchesDesc.optional
    )(BranchConfigOpt.apply, BranchConfigOpt.unapply)
}

sealed trait TagParts
object TagParts {
  case object Version extends TagParts
  //case object VersionPreRelease extends TagParts
  case object VersionBuildMetadata extends TagParts
  //case object VersionPreReleaseBuildMetadata extends TagParts

  def apply(tagParts: String): TagParts = {
    tagParts match {
      case "Version" => Version
      //case "VersionPreRelease" => VersionPreRelease
      case "VersionBuildMetadata" => VersionBuildMetadata
      //case "VersionPreReleaseBuildMetadata" => VersionPreReleaseBuildMetadata
    }
  }

  def unapply(arg: TagParts): Option[String] = Some(arg.toString)
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

case class AppConfig(defaults: BranchConfig, branches: List[BranchConfigOpt], patches: List[PatchConfig], formats: List[String])

object AppConfig {
  val appConfigDesc = (
      nested("defaults")(BranchConfig.branchConfigDesc) |@|
      nested("branches")(list(BranchConfig.branchConfigOptDesc)) |@|
      nested("patches")(list(PatchConfig.patchConfigDesc)) |@|
      list(string("formats")).default(Nil)
    )(AppConfig.apply, AppConfig.unapply)

  def getBranchConfig(currentBranch: String): BranchConfig = {
    val appConfig = getAppConfig()
    val defaults = appConfig.defaults

    val branchConfig = appConfig.branches.find { bc => currentBranch.matches(bc.name) }

    branchConfig.map { bc =>
      BranchConfig(
        name = bc.name,
        prefix = bc.prefix.getOrElse(defaults.prefix),
        tag = bc.tag.getOrElse(defaults.tag),
        tagParts = bc.tagParts.getOrElse(defaults.tagParts),
        tagMessageFormat = bc.tagMessageFormat.getOrElse(defaults.tagMessageFormat),
        preReleaseName = bc.preReleaseName.getOrElse(defaults.preReleaseName),
        buildMetadataFormat = bc.buildMetadataFormat.getOrElse(defaults.buildMetadataFormat),
        patches = bc.patches.getOrElse(defaults.patches)
      )
    }.getOrElse(defaults)
  }

  def getPatchConfigs(branchConfig: BranchConfig): List[PatchConfig] = {
    val allPatchConfigs = getAppConfig().patches.map(it => (it.name, it)).toMap
    branchConfig.patches.map(allPatchConfigs.get(_).orElse(sys.error("Can't find patch config")).get)
  }

  def getAppConfig(): AppConfig = {
    val hocon = if (File("mkver.conf").exists) {
      TypeSafeConfigSource.fromTypesafeConfig(ConfigFactory.parseFile(new java.io.File("mkver.conf")))
      // TODO Use this when in ZIO land
      // TypeSafeConfigSource.fromHoconFile(new java.io.File("mkver.conf"))
    } else {
      TypeSafeConfigSource.fromTypesafeConfig(ConfigFactory.load("application.conf"))
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
