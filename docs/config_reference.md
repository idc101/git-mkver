# Configuration

git-mkver comes with a default configuration. It can be overridden by creating a custom config file.

git-mkver will search for config in this order:

- file specified by the `-c` or `--configFile` command line argument
- file specified by the `GITMKVER_CONFIG` environment variable
- `mkver.conf` in the current working directory
  
The application uses the HOCON format. More details on the specification can be found
[here](https://github.com/lightbend/config/blob/master/HOCON.md).

## mkver.conf

```hocon
# Versioning mode to be used in this repository
# Can be one of SemVer | SemVerPreRelease | YearMonth | YearMonthPreRelease
mode: SemVer
# defaults are used if they are not overriden by a branch config
defaults {
  # format string to be used for versioning the repo
  # the version must be a valid SemVer so the format must be one of:
  # Version | VersionPreRelease | VersionBuildMetaData | VersionPreReleaseBuildMetaData
  versionFormat: VersionBuildMetaData
  # whether to really tag the branch when `git mkver tag` is called
  tag: false
  # prefix for tags in git
  tagPrefix: v
  # message for annotated version tags in git
  tagMessageFormat: "release {Tag}"
  # name of the pre-release e.g. alpha, beta, rc
  preReleaseName: "rc"
  # action to take if after analyzing all commit messages since the last tag
  # no increment instructions can be found. Options are:
  # * Fail - application will exit
  # * IncrementMajor - bump the major version
  # * IncrementMinor - bump the minor version
  # * IncrementPatch - bump the patch version
  # * NoIncrement - no version change will occur
  whenNoValidCommitMessages: "IncrementMinor"
  # list of patches to be applied when `git mkver patch` is called
  patches: [
    HelmChart
    Csproj
  ]
  # list of formats
  formats: [
    {
      name: BuildMetaData
      format: "{Branch}.{ShortHash}"
    }
    {
      name: Docker
      format: "{Version}"
    }
    {
      name: DockerBranch
      format: "{Version}.{Branch}.{ShortHash}"
    }
  ]
}
# branch specific overrides of the default config
# name is a regular expression
# branches are tried for matches in order
branches: [
  {
    name: "master"
    tag: true
    tagFormat: Version
  }
  {
    name: ".*"
    tag: false
    formats: [
      {
        name: Docker
        format: "{DockerBranch}"
      }
    ]
  }
]
# patches control how files are updated
patches: [
  {
    # name of the patch, referenced from the branch configs
    name: HelmChart
    # files to match, can include glob wildcards
    filePatterns: [
      "**Chart.yaml" # Chart.yaml in current working directory or any subdirectory of the current working directory
      "**/Chart.yaml" # Chart.yaml in any subdirectory of the current working directory
      "Chart.yaml" # Chart.yaml the current working directory only
    ]
    # search string, using java regular expression syntax (https://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html)
    find: "appVersion: .*"
    # replacement string using substitutions from formats
    replace: "appVersion: \"{Version}\""
  }
  {
    name: Csproj
    filePatterns: ["**/*.csproj"]
    find: "<Version>.*</Version>"
    replace: "<Version>{Version}</Version>"
  }
]
```