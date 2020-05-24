# Usage

Basic usage is to just call `git mkver next` and it will tell you the next
version of the software if you publish now.

```bash
$ git mkver next
0.4.0
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

## Tagging

If you would like to publish a version mkver can tag the current commit.

```bash
$ git mkver tag
```

This will apply an annotated tag from the `next` command to the current commit.

## Patching versions in files

If you would like to patch version numbers in files prior to building and tagging then
you can use the `patch` command. The files to be patched and the replacements are
defined in the `mkver.conf` [config](config) file.

```bash
$ git mkver patch
```

## Info

If you want to see all variables or configuration you can use the `info` command:

```bash
$ git mkver info
```