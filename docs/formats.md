# Format System

git-mkver includes a powerful string formatting system for creating version strings in different styles on different
branches. This is required as different software often have different restrictions on what a valid version number might be.

For example git is happy with the SemVer standard for tagging but docker does not support the `+` symbol in docker tags.

All replacements in format strings start with `{` and end with `}`. They are recursively replaced so that one may refer to another.

## Examples



## SemVer Formats

The following built in formats conform to the SemVer spec. They cannot be overriden.

| Format Token  | Substitution  |
| ------------- | ------------- |
| `Version` | `{x}.{y}.{z}` |
| `VersionPreRelease` | `{Version}-{PreRelease}` |
| `VersionBuildMetaData` | `{Version}+{BuildMetaData}` |
| `VersionPreReleaseBuildMetaData` | `{Version}-{PreRelease}+{BuildMetaData}` |


## Built-in Formats 

| Format Token  | Substitution  |
| ------------- | ------------- |
| `Next` | Full Semantic Version |
| `Tag` | Full Semantic Version as a tag (includes the prefix) |
| `TagMessage` | Tag Message |
| `x` | Version major number |
| `z` | Version patch number |
| `y` | Version minor number |
| `br` | Branch name |
| `sh` | Short Hash |
| `hash` | Full Hash |
| `dd` | Day |
| `mm` | Month |
| `yyyy` | Year |
| `bn` | Build No from build system |
| `tag?` | `true` if this branch is allowed to be tagged; `false` otherwise |
| `pr` | Tag prefix |
| `env.XXXX` | Environment Variables |

### Environment Variables
