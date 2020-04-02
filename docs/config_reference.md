# Configuration

git-mkver comes with a default configuration. It can be overriden by creating a `mkver.conf` file. git-mkver will search
for this file in the current working directory.
  
The application uses the HOCON format. More details on the specification can be found
[here](https://github.com/lightbend/config/blob/master/HOCON.md).

## mkver.conf

```hocon
defaults {
  prefix: v
  tagMessageFormat: "release %tag"
  tagFormat: version-buildmetadata
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
    tagFormat: version
  }
  {
    name: ".*"
    tag: false
    formats: [
      {
        name: docker
        format: "%docker-branch"
      }
    ]
  }
]
patches: [
  {
    name: helm-chart
    filePatterns: ["**/Chart.yaml"]
    find: "version: .*"
    replace: "version: \"%version\""
  }
  {
    name: csproj
    filePatterns: ["**/*.csproj"]
    find: "<Version>.*</Version>"
    replace: "<Version>%version</Version>"
  }
]
formats: [
  {
    name: buildmetadata
    format: "%br.%sh"
  }
  {
    name: docker
    format: "%version"
  }
  {
    name: docker-branch
    format: "%version.%br.%sha"
  }
]
```