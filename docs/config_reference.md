# Configuration

git-mkver comes with a default configuration. It can be overriden by creating a `mkver.conf` file. git-mkver will search
for this file in the current working directory.
  
The application uses the HOCON format. More details on the specification can be found
[here](https://github.com/lightbend/config/blob/master/HOCON.md).

## mkver.conf

```hocon
defaults {
  prefix: v
  tagMessageFormat: "release {tag}"
  tagFormat: VersionBuildMetaData
  patches: [
    HelmChart
    Csproj
  ]
}
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
patches: [
  {
    name: HelmChart
    filePatterns: ["**/Chart.yaml"]
    find: "version: .*"
    replace: "version: \"{Version}\""
  }
  {
    name: Csproj
    filePatterns: ["**/*.csproj"]
    find: "<Version>.*</Version>"
    replace: "<Version>{Version}</Version>"
  }
]
formats: [
  {
    name: BuildMetaData
    format: "{br}.{sh}"
  }
  {
    name: Docker
    format: "{Version}"
  }
  {
    name: DockerBranch
    format: "{Version}.{br}.{sh}"
  }
]
```