package net.cardnell.mkver

object Formatter {
  case class Formatter(formats: List[Format]) {
    def format(input: String, count: Int = 0): String = {
      if (count > 100) {
        // likely an infinite loop
        input
      } else {
        val result = formats.sortBy(_.name.length * -1).foldLeft(input) { (s, v) =>
          s.replace("{" + v.name + "}", v.format)
        }
        if (result == input) {
          // no replacements made - we are done
          result
        } else {
          // recursively replace
          format(result, count + 1)
        }
      }
    }
  }

  def apply(version: VersionData, runConfig: RunConfig, preRelease: Boolean): Formatter = {
    val versionFormat = (preRelease, runConfig.includeBuildMetaData) match {
      case (false, false) => "{Version}"
      case (true, false) => "{VersionPreRelease}"
      case (false, true) => "{VersionBuildMetaData}"
      case (true, true) => "{VersionPreReleaseBuildMetaData}"
    }
    Formatter(List(
      Format("Version", "{Major}.{Minor}.{Patch}"),
      Format("VersionPreRelease", "{Version}-{PreRelease}"),
      Format("VersionBuildMetaData", "{Version}+{BuildMetaData}"),
      Format("VersionPreReleaseBuildMetaData", "{Version}-{PreRelease}+{BuildMetaData}"),
      Format("BuildMetaData", runConfig.buildMetaDataFormat),
      Format("Next", versionFormat),
      Format("Tag", "{TagPrefix}{Next}"),
      Format("TagMessage", runConfig.tagMessageFormat),
      Format("PreRelease", runConfig.preReleaseFormat),
      Format("PreReleaseNumber", version.preReleaseNumber.map(_.toString).getOrElse("")),
      Format("Major", version.major.toString),
      Format("Minor", version.minor.toString),
      Format("Patch", version.patch.toString),
      Format("Branch", branchNameToVariable(version.branch)),
      Format("ShortHash", version.commitHashShort),
      Format("FullHash", version.commitHashFull),
      Format("CommitsSinceTag", version.commitCount.toString),
      Format("Tagged?", (version.commitCount == 0).toString),
      Format("dd", version.date.getDayOfMonth.formatted("%02d")),
      Format("mm", version.date.getMonthValue.formatted("%02d")),
      Format("yyyy", version.date.getYear.toString),
      Format("Tag?", runConfig.tag.toString),
      Format("TagPrefix", runConfig.tagPrefix)
    ) ++ envVariables()
      ++ azureDevOpsVariables())
  }

  def azureDevOpsVariables(): List[Format] = {
    // These need special formatting to be useful
    List(
      envVariable("SYSTEM_PULLREQUEST_SOURCEBRANCH", "AzurePrSourceBranch")
        .map( f => f.copy(format = branchNameToVariable(f.format)))
    ).flatten
  }

  def envVariables(): List[Format] = {
    sys.env.map { case (name, value) => Format(s"env.$name", value) }.toList
  }

  def envVariable(name: String, formatName: String): Option[Format] = {
    sys.env.get(name).map { value => Format(formatName, value) }
  }

  def branchNameToVariable(branchName: String): String = {
    branchName
      .replace("refs/heads/", "")
      .replace("refs/", "")
      .replace("/", "-")
      .replace("_", "-")
  }
}
