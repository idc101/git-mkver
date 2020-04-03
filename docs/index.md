## Features

- Determine next version based on:
    - Last tagged commit
    - [Conventional Commits](https://www.conventionalcommits.org/)
    - Branch names
    - Manual tagging
- Next version conforms to [Semantic Versioning](https://semver.org/) scheme
- Patch the next version into the build:
    - Java
    - C#
    - Many others, fully configurable
- Tag the current commit with the next version

All of this can be configured based on the branch name so release/master branches get different
version numbers to develop or feature branches.

## Getting started

[Install](installation) the binary and then read through the usage. 