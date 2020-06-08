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
# prefix for tags in git
tagPrefix: v
# defaults are used if they are not overriden by a branch config
defaults {
  # whether to really tag the branch when `git mkver tag` is called
  tag: false
  # message for annotated version tags in git
  tagMessageFormat: "release {Tag}"
  # format tring for the pre-release. The format must end with {PreReleaseNumber} if it is used.
  # Examples:
  # * alpha
  # * SNAPSHOT
  # * RC{PreReleaseNumber}
  # * pre-{CommitsSinceTag}
  preReleaseFormat: "RC{PreReleaseNumber}"
  # format string to be used for the build metadata
  buildMetaDataFormat: "{Branch}.{ShortHash}"
  # whether to include the build metadata in the Semantic Version when next or tag are called
  includeBuildMetaData: true
  # action to take, if after analyzing all commit messages since the last tag
  # no increment instructions can be found. Options are:
  # * Fail - application will exit
  # * IncrementMajor - bump the major version
  # * IncrementMinor - bump the minor version
  # * IncrementPatch - bump the patch version
  # * NoIncrement - no version change will occur
  whenNoValidCommitMessages: IncrementMinor
  # list of patches to be applied when `git mkver patch` is called
  patches: [
    HelmChart
    Csproj
  ]
  # list of formats
  formats: [
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
    pattern: "master"
    tag: true
    includeBuildMetaData: false
  }
  {
    pattern: ".*"
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
    # list of replacements to apply to files
    replacements: [
      {
        # search string, using java regular expression syntax (https://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html)
        # find strings can include the special marker `{VersionRegex}` which will be replaced with the regular expression
        # for a Semantic Version.
        find: "appVersion: {VersionRegex}"
        # replacement string using substitutions from formats
        replace: "appVersion: \"{Version}\""
      }
    ]
  }
  {
    name: Csproj
    filePatterns: ["**/*.csproj"]
    replacements: [
      {
        find: "<Version>.*</Version>"
        replace: "<Version>{Version}</Version>"
      }
    ]
  }
]
# commitMessageActions configure how different commit messages will increment
# the version number
commitMessageActions: [
  {
    # pattern is a regular expression to occur in a single line
    pattern: "BREAKING CHANGE"
    # action is one of:
    # * Fail - application will exit
    # * IncrementMajor - bump the major version
    # * IncrementMinor - bump the minor version
    # * IncrementPatch - bump the patch version
    # * NoIncrement - no version change will occur
    action: IncrementMajor
  }
  {
    pattern: "major(\\(.+\\))?:"
    action: IncrementMajor
  }
]
```