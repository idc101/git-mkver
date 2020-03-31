package net.cardnell.mkver

object VariableReplacer {
  case class Variable(name: String, variable: String, actual: String)

  case class VariableReplacer(variables: List[Variable]) {
    def replace(input: String): String = {
      variables.foldLeft(input) { (s, v) =>
        s.replace(v.variable, v.actual)
      }
    }
  }

  def apply(version: VersionData): VariableReplacer = {
    VariableReplacer(List(
      Variable("version", "%ver", s"${version.major}.${version.minor}.${version.patch}"),
      Variable("major", "%x", version.major.toString),
      Variable("minor", "%y", version.minor.toString),
      Variable("patch", "%z", version.patch.toString),
      Variable("branch", "%br", version.branch.replace("/", "-")),
      Variable("shortHash", "%sh", version.commitHashShort),
      Variable("fullHash", "%hash", version.commitHashFull),
      Variable("day", "%dd", version.date.getDayOfMonth.formatted("00")),
      Variable("month", "%mm", version.date.getMonthValue.formatted("00")),
      Variable("year", "%yyyy", version.date.getYear.toString),
      Variable("buildNo", "%bn", version.buildNo)
    ))
  }
}
