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
    find: "\\d+\\.\\d+\\.\\d+"
    replace: "{Next}"
  }
]
```

# I would like a different version format for docker tags

Docker does not support `+` symbols from semantics versions. Create a
format for Docker tags. Additionally `master` branch should be a Version
only while other branches should include build metadata.
