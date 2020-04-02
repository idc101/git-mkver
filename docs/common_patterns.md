# Usage Patterns

## trunk/master based development

Developers commit to master or work on feature branches.

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
# Publish artifacts
```

To control the frequency of releases, include these steps only on manually
triggered builds.
