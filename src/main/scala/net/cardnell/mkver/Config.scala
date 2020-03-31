package net.cardnell.mkver

import scala.util.matching.Regex

case class BranchConfig(name: Regex,
                        prefix: String,
                        tag: Boolean,
                        tagParts: TagParts,
                        tagMessageFormat: String,
                        preReleaseName: String,
                        buildMetadataFormat: String,
                        patches: List[PatchConfig])

sealed trait TagParts
object TagParts {
  case object Version extends TagParts
  case object VersionPreRelease extends TagParts
  case object VersionBuildMetadata extends TagParts
  case object VersionPreReleaseBuildMetadata extends TagParts
}

case class PatchConfig(val name: String, val filePatterns: List[String], val findRegex: String, val replace: String)

object Config {

}
