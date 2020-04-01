# Configuration

git-mkver comes with a default configuration. It can be overriden by creating a `mkver.conf` file. git-mkver will search
for this file in the current working directory.
  
The application uses the HOCON format. More details on the specification can be found
[here][https://github.com/lightbend/config/blob/master/HOCON.md].

## mkver.conf

```hocon
# d
defaults {
  prefix: v
  tagMessageFormat: "release %ver - buildno: %bn"
  tagParts: VersionBuildMetadata
  #minimumVersionIncrement: Major|Minor|Patch|PreRelease|None
  patches: [
    helm-chart
    csproj
  ]
}
branches: [
  {
    name: "master"
    tag: true
    tagParts: Version
  }
  {
    name: ".*"
    tag: false
  }
]
patches: [
  {
    name: helm-chart
    filePatterns: ["**/Chart.yaml"]
    find: "version: .*"
    replace: "version: \"%ver\""
  }
  {
    name: csproj
    filePatterns: ["**/*.csproj"]
    find: "<Version>.*</Version>"
    replace: "<Version>%ver</Version>"
  }
]
```