#  git-mkver

Git-mkver uses git tags, branch names and commit messages to determine  
the next version of the software to release.


## Installation

Download the binary for your os from the releases page and copy to
somewhere on your path.

## Usage

Basic usage is to just call `git mkver next` and it will tell you the next
version of the software if you publish now.

```
$ git mkver next
0.4.0
```

### Tagging

If you would like to publish a version mkver can tag the current commit.

```
$ git mkver tag
```

This will apply an annotated tag from the `next` command to the current commit.

### Patching versions in files

If you would like to patch version numbers in files prior to building and tagging then
you can use the `patch` command. The files to be patched and the replacements are
defined in the `mkver.yaml` config file. A large number of standard patches come
pre-defined.

```
$ git mkver patch
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
