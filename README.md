#  git-mkver

Git-mkver uses git tags, branch names and commit messages to determine  
the next version of the software to release.


## Installation

Download the binary for your os from the releases page and copy to
somewhere on your path.

## Usage

Basic usage is to just call `git mkver` and it will tell you the next
version of the software if you publish now.
```
$ git mkver
0.4.0
```

### Usage Patterns


Developers commit to master or work on feature branches:

- Any commit containing `feat:` will bump the minor version
- Any commit containing `fix:` will bump the patch version

The build script run by the build server would look something like:

```
nextVer=$(git mkver next)
git tag -a -m "New Version" "v$nextVer"
# Publish artifacts
```

To control the frequency of releases, include these steps only on manually
triggered builds.
