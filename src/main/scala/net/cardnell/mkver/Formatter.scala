package net.cardnell.mkver

object Formatter {
  val builtInFormats = List(
    Format("version", "%x.%y.%z"),
    Format("version-prerelease", "%version-%prerelease"),
    Format("version-buildmetadata", "%version+%buildmetadata"),
    Format("version-prerelease-buildmetadata", "%version-%prerelease+%buildmetadata"),
  )

  val defaultFormats = List(
    Format("prerelease", "rc-%prereleasename"),
    Format("buildmetadata", "%br.%sh"),
  )

  case class Formatter(formats: List[Format]) {
    def format(input: String): String = {
      val result = formats.sortBy(_.name.length * -1).foldLeft(input) { (s, v) =>
        s.replace("%" + v.name, v.format)
      }
      if (result == input) {
        // no replacements made - we are done
        result
      } else {
        // recursively replace
        format(result)
      }
    }
  }

  def apply(version: VersionData, branchConfig: BranchConfig): Formatter = {
    Formatter(List(
      Format("x", version.major.toString),
      Format("y", version.minor.toString),
      Format("z", version.patch.toString),
      Format("br", version.branch.replace("/", "-")),
      Format("sh", version.commitHashShort),
      Format("hash", version.commitHashFull),
      Format("dd", version.date.getDayOfMonth.formatted("00")),
      Format("mm", version.date.getMonthValue.formatted("00")),
      Format("yyyy", version.date.getYear.toString),
      Format("bn", version.buildNo),
      Format("tag?", branchConfig.tag.toString),
      Format("pr", branchConfig.prefix.toString)
    ) ++ branchConfig.formats)
  }
}
