# git mkver: Automatic Semantic Versioning

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
- Works with all branching strategies:
    - [master/trunk based development](https://trunkbaseddevelopment.com/)
    - [Git Flow](https://nvie.com/posts/a-successful-git-branching-model/) 
    - [GitHub Flow](https://guides.github.com/introduction/flow/)

All of this can be configured based on the branch name so release/master
branches get different version numbers to develop or feature branches.

## Getting started

[Install](installation) the binary and then read through the [usage](usage).

## Related Projects

* [Github Setup Action for git-mkver](https://github.com/cperezabo/setup-git-mkver)
