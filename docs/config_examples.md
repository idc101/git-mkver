# Config Examples

Below are some examples of common configuration.

## Patching README.md

I would like to update my README.md and others docs with the latest version number.

```hocon
defaults {
  patches: [
    Docs
  ]
}
patches: [
  {
    name: Docs
    filePatterns: [
      "README.md"
      "docs/installation.md"
    ]
    replacements: [
      {
        find: "\\d+\\.\\d+\\.\\d+"
        replace: "{Next}"
      }
    ]
  }
]
```

# I would like a different version format for docker tags

Docker does not support the `+` symbol from semantics versions. Create a
format for Docker tags like so:

```hocon
branches: [
  {
    pattern: "master"
    includeBuildMetaData: false
    tag: true
    formats: [
      { name: Docker, format: "{Version}" }
    ]
  }
  {
    pattern: ".*"
    formats: [
      { name: Docker, format: "{Version}-{BuildMetaData}" }
    ]
  }
]
```

Generate it with `git mkver next --format '{Docker}'`
