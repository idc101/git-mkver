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

## I would like to override the built-in commitMessageActions

git-mkver includes a default list of commit message actions which map to
[conventional commits](https://www.conventionalcommits.org/en/v1.0.0/). These can be overridden by specifying the
pattern to override in the `commitMessageActions`. The pattern must match exactly one of the patterns below and then
the `action` can be changed as required.

The defaults are as follows:

```hcon
commitMessageActions: [
  # Breaking changes (major)
  {
    pattern: "BREAKING CHANGE"
    action: IncrementMajor
  }
  {
    pattern: "major(\\(.+\\))?!:"
    action: IncrementMajor
  }
  {
    pattern: "minor(\\(.+\\))?!:"
    action: IncrementMajor
  }
  {
    pattern: "patch(\\(.+\\))?!:"
    action: IncrementMajor
  }
  {
    pattern: "feature(\\(.+\\))?!:"
    action: IncrementMajor
  }
  {
    pattern: "feat(\\(.+\\))?!:"
    action: IncrementMajor
  }
  {
    pattern: "fix(\\(.+\\))?!:"
    action: IncrementMajor
  }
  # The rest of the conventional commits
  {
    pattern: "(build|ci|chore|docs|perf|refactor|revert|style|test)(\\(.+\\))?!:"
    action: IncrementMajor
  }
  {
    pattern: "major(\\(.+\\))?:"
    action: IncrementMajor
  }
  {
    pattern: "minor(\\(.+\\))?:"
    action: IncrementMinor
  }
  {
    pattern: "patch(\\(.+\\))?:"
    action: IncrementPatch
  }
  {
    pattern: "feature(\\(.+\\))?:"
    action: IncrementMinor
  }
  {
    pattern: "feat(\\(.+\\))?:"
    action: IncrementMinor
  }
  {
    pattern: "fix(\\(.+\\))?:"
    action: IncrementPatch
  }
  # The rest of the conventional commits
  {
    pattern: "(build|ci|chore|docs|perf|refactor|revert|style|test)(\\(.+\\))?:"
    action: NoIncremen
  }
```
