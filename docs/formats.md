# Format System

git-mkver includes a powerful string formatting system for creating version strings in different styles on different
branches. This is required as different software often have different restrictions on what a valid version number might be.

For example git is happy with the SemVer standard for tagging but docker does not support the `+` symbol in docker tags.

All replacements in format strings start with `{` and end with `}`. They are recursively replaced so that one may refer to another.

## Examples



## Built-in Formats 

| Format Token  | Substitution  |
| ------------- | ------------- |
| `x` | Version major number |
| `z` | Version patch number |
| `y` | Version minor number |
| `br` | Branch name |
| `sh` | Short Hash |
| `hash` | Full Hash |
| `dd` | Day |
| `mm` | MonthValue, |
| `yyyy` | Year, |
| `bn` | Build No from build system |
| `tag` | Full tag |
| `pr` | Tag prefix |