use clap::{Parser, Subcommand};
use crate::GIT_MKVER_VERSION;

#[derive(Debug, Clone, PartialEq, Eq, Parser)]
#[command(
    name = "git mkver",
    version = GIT_MKVER_VERSION,
    about = format!("git-mkver - v{}\n\nUses git tags, branch names and commit messages to determine the next version of the software to release", GIT_MKVER_VERSION)
)]
pub struct CommandLineOpts {
    /// Config file to load
    #[arg(short = 'c', long = "config", value_name = "file", global = true)]
    pub config_file: Option<String>,

    #[command(subcommand)]
    pub opts: AppOpts,
}

#[derive(Debug, Clone, PartialEq, Eq, Subcommand)]
pub enum AppOpts {
    /// Print the next version tag that would be used
    Next(NextOpts),
    /// Git Tag the current commit with the next version tag
    Tag(TagOpts),
    /// Patch version information in files with the next version tag
    Patch(PatchOpts),
    /// output all formats and branch configuration
    Info(InfoOpts),
    /// output final configuration to be used
    Config,
}

#[derive(Debug, Clone, PartialEq, Eq, clap::Args)]
pub struct NextOpts {
    /// Format string for the version number
    #[arg(short = 'f', long = "format", value_name = "string")]
    pub format: Option<String>,

    /// Include the tag prefix in the output
    #[arg(short = 'p', long = "pre-release")]
    pub pre_release: bool,

    /// Include the tag prefix in the output
    #[arg(short = 't', long = "tag-prefix")]
    pub prefix: bool,
}

#[derive(Debug, Clone, PartialEq, Eq, clap::Args)]
pub struct TagOpts {
    /// Include the tag prefix in the output
    #[arg(short = 'p', long = "pre-release")]
    pub pre_release: bool,
}

#[derive(Debug, Clone, PartialEq, Eq, clap::Args)]
pub struct PatchOpts {
    /// Include the tag prefix in the output
    #[arg(short = 'p', long = "pre-release")]
    pub pre_release: bool,
}

#[derive(Debug, Clone, PartialEq, Eq, clap::Args)]
pub struct InfoOpts {
    /// Include the tag prefix in the output
    #[arg(short = 'p', long = "pre-release")]
    pub pre_release: bool,

    /// Include environment variables
    #[arg(short = 'i', long = "include-env")]
    pub include_env: bool,
}

impl CommandLineOpts {
    pub fn parse_from_args<I, T>(args: I) -> Result<Self, String>
    where
        I: IntoIterator<Item = T>,
        T: Into< std::ffi::OsString> + Clone,
    {
        let mut iter = args.into_iter().map(|x| x.into()).collect::<Vec<_>>();
        iter.insert(0, "git mkver".into());
        Self::try_parse_from(iter).map_err(|e| e.to_string())
    }
}
