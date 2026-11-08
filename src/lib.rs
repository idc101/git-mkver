pub mod app_config;
pub mod cli;
pub mod files;
pub mod formatter;
pub mod git;
pub mod mkver;
pub mod version;

pub const GIT_MKVER_VERSION: &str = "2.0.0";

use std::path::Path;
use regex::Regex;
use crate::app_config::{get_run_config, RunConfig};
use crate::cli::{AppOpts, CommandLineOpts, InfoOpts, NextOpts, PatchOpts, TagOpts};
use crate::formatter::Formatter;
use crate::git::Git;
use crate::mkver::{format_version, get_next_version};
use crate::version::VERSION_FULL_REGEX;

pub trait Console {
    fn put_str_ln(&mut self, line: &str);
}

#[derive(Default)]
pub struct StdConsole;

impl Console for StdConsole {
    fn put_str_ln(&mut self, line: &str) {
        println!("{}", line);
    }
}

pub fn main_impl(args: &[String], git: &dyn Git, console: &mut dyn Console) -> Result<(), String> {
    let opts = CommandLineOpts::parse_from_args(args)?;
    run(&opts, git, console)
}

pub fn run(opts: &CommandLineOpts, git: &dyn Git, console: &mut dyn Console) -> Result<(), String> {
    git.check_git_repo()?;
    let current_branch = git.current_branch()?;
    let config = get_run_config(opts.config_file.as_deref(), &current_branch)?;
    match &opts.opts {
        AppOpts::Next(next_opts) => run_next(next_opts, &config, &current_branch, git, console),
        AppOpts::Tag(tag_opts) => run_tag(tag_opts, &config, &current_branch, git),
        AppOpts::Patch(patch_opts) => run_patch(patch_opts, &config, &current_branch, git, console),
        AppOpts::Info(info_opts) => run_info(info_opts, &config, &current_branch, git, console),
        AppOpts::Config => run_config(&config, console),
    }
}

pub fn run_next(
    next_opts: &NextOpts,
    config: &RunConfig,
    current_branch: &str,
    git: &dyn Git,
    console: &mut dyn Console,
) -> Result<(), String> {
    let next_version = get_next_version(git, config, current_branch, next_opts.pre_release)?;
    let next = if let Some(ref format) = next_opts.format {
        Formatter::from_version(&next_version, config, next_opts.pre_release).format(format)
    } else {
        format_version(config, &next_version, next_opts.prefix, next_opts.pre_release)
    };
    console.put_str_ln(&next);
    Ok(())
}

pub fn run_tag(
    tag_opts: &TagOpts,
    config: &RunConfig,
    current_branch: &str,
    git: &dyn Git,
) -> Result<(), String> {
    let next_version = get_next_version(git, config, current_branch, tag_opts.pre_release)?;
    let tag = format_version(config, &next_version, true, tag_opts.pre_release);
    let tag_message =
        Formatter::from_version(&next_version, config, tag_opts.pre_release).format(&config.tag_message_format);
    if config.tag {
        git.tag(&tag, &tag_message)?;
    }
    Ok(())
}

pub fn run_patch(
    patch_opts: &PatchOpts,
    config: &RunConfig,
    current_branch: &str,
    git: &dyn Git,
    console: &mut dyn Console,
) -> Result<(), String> {
    let next_version = get_next_version(git, config, current_branch, patch_opts.pre_release)?;
    for patch in &config.patches {
        for find_replace in &patch.replacements {
            let pattern = find_replace.find.replace("{VersionRegex}", VERSION_FULL_REGEX);
            let regex = Regex::new(&pattern).map_err(|e| e.to_string())?;
            let replacement =
                Formatter::from_version(&next_version, config, patch_opts.pre_release)
                    .format(&find_replace.replace);
            for file_pattern in &patch.file_patterns {
                let cwd = Path::new("");
                let matches = files::glob(cwd, file_pattern)?;
                for file_match in matches {
                    console.put_str_ln(&format!(
                        "Patching file: '{}', new value: '{}'",
                        file_match.display(),
                        replacement
                    ));
                    let content = files::read_all(&file_match)?;
                    let new_content = regex.replace_all(&content, replacement.as_str());
                    files::write(&file_match, &new_content)?;
                }
            }
        }
    }
    Ok(())
}

pub fn run_info(
    info_opts: &InfoOpts,
    config: &RunConfig,
    current_branch: &str,
    git: &dyn Git,
    console: &mut dyn Console,
) -> Result<(), String> {
    let next_version = get_next_version(git, config, current_branch, info_opts.pre_release)?;
    let formatter = Formatter::from_version(&next_version, config, info_opts.pre_release);
    for format in &formatter.formats {
        if !format.name.starts_with("env") || info_opts.include_env {
            let result = formatter.format(&format.format);
            console.put_str_ln(&format!("{}={}", format.name, result));
        }
    }
    Ok(())
}

pub fn run_config(config: &RunConfig, console: &mut dyn Console) -> Result<(), String> {
    console.put_str_ln(&config.to_hocon_string());
    Ok(())
}
