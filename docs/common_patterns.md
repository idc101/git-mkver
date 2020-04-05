# Usage Patterns

## trunk/master based development

This mode of operation is the default
Overview:

- Developers commit to master or work on feature branches.
- All releases are done from the master branch.
- Only the master branch is tagged
- Release Candidates are not used
-  Any version number not from master includes build metadata to indicate it is not an official release.

### Controlling the next version number

The next version number is obtained by parsing the commit messages since the last version was tagged.

- Commits containing the following will bump with _major_ version:
  - `major:` or `major(...):`
  - `BREAKING CHANGE`
- Commits containing the following will bump with _minor_ version:
  - `minor:` or `minor(...):`
  - `feat:` or `feat(...):`
- Commits containing the following will bump with _patch_ version:
  - `patch:` or `patch(...):`
  - `fix:` or `fix(...):`

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

TODO

## GitHub flow

TODO

## 