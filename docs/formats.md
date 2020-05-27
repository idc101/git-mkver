# Format System

git-mkver includes a powerful string formatting system for creating version strings in different styles on different
branches. This is required as different software often have different restrictions on what a valid version number might be.

For example git is happy with the SemVer standard for tagging but docker does not support the `+` symbol in docker tags.

All replacements in format strings start with `{` and end with `}`. They are recursively replaced so that one may refer to another.

## SemVer Formats

The following built in formats conform to the SemVer spec. They cannot be overriden.

| Format Token  | Substitution  |
| ------------- | ------------- |
| `Version` | `{Major}.{Minor}.{Patch}` |
| `VersionPreRelease` | `{Version}-{PreRelease}` |
| `VersionBuildMetaData` | `{Version}+{BuildMetaData}` |
| `VersionPreReleaseBuildMetaData` | `{Version}-{PreRelease}+{BuildMetaData}` |

## Built-in Formats 

| Format Token  | Substitution  |
| ------------- | ------------- |
| `Next` | Full Semantic Version |
| `Tag` | Full Semantic Version as a tag (includes the prefix) |
| `TagMessage` | Tag Message |
| `Major` | Version major number |
| `Minor` | Version minor number |
| `Patch` | Version patch number |
| `PreRelease` | Pre-release |
| `BuildMetaData` | BuildMetaData |
| `Branch` | Branch name |
| `ShortHash` | Short Hash |
| `FullHash` | Full Hash |
| `CommitsSinceTag` | Number of commits since last tag |
| `Tagged?` | `true` if this commit is tagged (`CommitsSinceTag` == 0), `false` otherwise |
| `dd` | Day |
| `mm` | Month |
| `yyyy` | Year |
| `Tag?` | `true` if this branch is allowed to be tagged; `false` otherwise |
| `Prefix` | Tag prefix |
| `env.XXXX` | Environment Variables |

### Environment Variables

All environment variables are available under a set of formats prefixed with `env.`.
For example `{env.BUILD_NUMBER}` would get the `BUILD_NUMBER` environment variable.
This is most useful for getting information from build systems.
