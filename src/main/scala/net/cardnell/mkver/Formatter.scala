package net.cardnell.mkver

object Formatter {
  val builtInFormats = List(
    Format("Version", "{x}.{y}.{z}"),
    Format("VersionPreRelease", "{Version}-{PreRelease}"),
    Format("VersionBuildMetaData", "{Version}+{BuildMetaData}"),
    Format("VersionPreReleaseBuildMetaData", "{Version}-{PreRelease}+{BuildMetaData}"),
  )

  val defaultFormats = List(
    Format("PreRelease", "rc-{PreReleaseName}"),
    Format("BuildMetaData", "{br}.{sh}"),
  )

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

  def apply(version: VersionData, branchConfig: BranchConfig): Formatter = {
    Formatter(List(
      Format("x", version.major.toString),
      Format("y", version.minor.toString),
      Format("z", version.patch.toString),
      Format("br", branchNameToVariable(version.branch)),
      Format("sh", version.commitHashShort),
      Format("hash", version.commitHashFull),
      Format("dd", version.date.getDayOfMonth.formatted("00")),
      Format("mm", version.date.getMonthValue.formatted("00")),
      Format("yyyy", version.date.getYear.toString),
      Format("bn", version.buildNo),
      Format("tag?", branchConfig.tag.toString),
      Format("pr", branchConfig.prefix.toString)
    ) ++ branchConfig.formats
      ++ envVariables()
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
      .replace("/", "_")
  }
}
