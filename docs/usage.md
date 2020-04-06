# Usage

Basic usage is to just call `git mkver next` and it will tell you the next
version of the software if you publish now.

```bash
$ git mkver next
0.4.0
```

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
defined in the `mkver.yaml` config file. A large number of standard patches come
pre-defined.

```bash
$ git mkver patch
```

## Info

If you want to see all variables or configuration you can use the `info` command:

```bash
$ git mkver info
```