# Usage Patterns

Below are some popular git branching development models and how to configure them with git-mkver:
- master (aka trunk) based development
- Git flow
- GitHub flow

### Controlling the next version number

Regardless of the branching strategy, git-mkver uses the commit messages to determine the next version number. 

See [Usage](usage) for more details.

## master (aka trunk) based development

This mode of operation works out of the box with the default configuration.

Overview:

- Developers commit to master or work on feature branches
- All releases are done from the master branch
- Only the master branch is tagged
- Release Candidates are not used
- Any version number not from master includes build metadata to indicate it is not an official release

### Build Server Setup

The build script run by the build server would look something like:

```bash
nextVer=$(git mkver next)
# build software ...
# If successful:
git mkver tag
# Publish artifacts and push tag
```

To control the frequency of releases, include these steps only on manually triggered builds.

## Git flow

[Git Flow](https://nvie.com/posts/a-successful-git-branching-model/) is a long standing and popular branching model.

## GitHub flow

[GitHub Flow](https://guides.github.com/introduction/flow/) is a newer, simplified versioning model developed by GitHub.
