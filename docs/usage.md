# Usage

Basic usage is to just call `git mkver next` and it will tell you the next
version of the software if you publish now.

```bash
$ git mkver next
0.4.0
```

Typical usages for `next`:
- Developers can run this locally to check the what the next version is
according to the commit message log
- This command can be run at the beginning of an automated build to get
a version number for use in built artifacts

## Tagging

If you would like to publish a version mkver can tag the current commit.

```bash
$ git mkver tag
```

This will apply an annotated tag from the `next` command to the current commit.

This would typically be called at the end of an automated build once all tests
have passed and artifacts have been successfully uploaded. This marks a successful
release in the git repository,

## Pre-releases

Pre-release versions are often called alpha, beta, RC (Release Candidate) or SNAPSHOT. For
example:

- 1.0.0-RC2
- 2.5.0-alpha

They denote and upcoming version that will soon be released. Pre-release versions are
usually released to users for testing and therefore it is useful to tag them.

It is a human decision as to when to release a pre-release version and at what point sufficient
testing has been done to release a final version of that pre-release. This could be done
locally by a developer and the tag pushed or there could be two build pipelines, one for
pre-release versions and another for final versions.

To create a pre-release the `next` and `tag` commands take a `--pre-release` (`-p`) flag which
will create a version number with the pre-release.

```bash
# Assuming the version is current 1.5.0
# Commit the next major release
$ git commit -m "major: big new changes"
$ git mkver tag --pre-release
2.0.0-RC1
# Found a bug...
$ git commit -m "fix: bug"
$ git mkver tag --pre-release
2.0.0-RC2
# We're all happy make this the final version
$ git mkver tag
2.0.0
```

## Controlling the next version number

The next version number will be determined based on the commit messages since
the last version was tagged. The commit messages that trigger different version
increments are [configurable](config_reference) but by default they are as follows:

- Commits containing the following will increment the _major_ version:
  - `major:` or `major(...):`
  - `BREAKING CHANGE`
- Commits containing the following will increment the _minor_ version:
  - `minor:` or `minor(...):`
  - `feat:` or `feat(...):`
- Commits containing the following will increment the _patch_ version:
  - `patch:` or `patch(...):`
  - `fix:` or `fix(...):`

All commit messages since the last tagged message are analyzed and the greatest
version increment is used. For example if one commit is a minor change and one is
a major change then the major version will be incremented.

## Common arguments

All commands take a `-c <FILE>` or `--config <FILE>` option to set the config file to read. More details on the
[Config Reference](config_reference) page.

## Patching versions in files

If you would like to patch version numbers in files prior to building and tagging then
you can use the `patch` command. The files to be patched and the replacements are
defined in the `mkver.conf` [config](config) file.

For example, suppose you have the version number in a code file:
```
object VersionInfo {
    val version = "1.0.0"
}
```

and you define a patch as follows in your config file:
```hocon
  {
    name: Readme
    filePatterns: ["version.scala"]
    find: "val version = \".*\""
    replace: "val version = \"{Next}\""
  }
```

you could update the code automatically as follows:
```bash
$ git mkver patch
```

## Info

If you want to see all format variables you can use the `info` command:

```bash
$ git mkver info
```
