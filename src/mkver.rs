use chrono::Local;
use regex::Regex;
use crate::app_config::{CommitMessageAction, RunConfig};
use crate::formatter::Formatter;
use crate::git::Git;
use crate::version::{IncrementAction, NextVersion, Version, VersionBumps, VersionData};

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct CommitInfo {
    pub short_hash: String,
    pub full_hash: String,
    pub commits_before_head: u32,
    pub tags: Vec<Version>,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct LastVersion {
    pub commit_hash: String,
    pub commits_before_head: u32,
    pub version: Version,
}

pub fn get_commit_infos(git: &dyn Git, prefix: &str) -> Result<Vec<CommitInfo>, String> {
    let line_match = Regex::new(r"^([0-9a-f]{5,40}) ([0-9a-f]{5,40}) *(\((.*)\))?$").unwrap();
    let log = git.commit_info_log()?;

    let mut commit_infos = Vec::new();
    for (i, line) in log.lines().enumerate() {
        if let Some(caps) = line_match.captures(line) {
            let short_hash = caps[1].to_string();
            let long_hash = caps[2].to_string();
            let names = caps.get(4).map(|m| m.as_str()).unwrap_or("");
            let versions: Vec<Version> = names
                .split(',')
                .map(|s| s.trim())
                .filter(|s| s.starts_with("tag: "))
                .map(|s| s.replacen("tag: ", "", 1))
                .filter_map(|tag| Version::parse_tag(&tag, prefix))
                .collect();
            commit_infos.push(CommitInfo {
                short_hash,
                full_hash: long_hash,
                commits_before_head: i as u32,
                tags: versions,
            });
        }
    }
    Ok(commit_infos)
}

pub fn get_last_version(commit_infos: &[CommitInfo]) -> Option<LastVersion> {
    commit_infos
        .iter()
        .find(|ci| !ci.tags.is_empty())
        .and_then(|ci| {
            ci.tags.iter().max().map(|max_ver| LastVersion {
                commit_hash: ci.full_hash.clone(),
                commits_before_head: ci.commits_before_head,
                version: max_ver.clone(),
            })
        })
}

pub fn format_version(
    config: &RunConfig,
    version_data: &VersionData,
    format_as_tag: bool,
    pre_release: bool,
) -> String {
    let formatter = Formatter::from_version(version_data, config, pre_release);
    if format_as_tag {
        formatter.format("{Tag}")
    } else {
        formatter.format("{Next}")
    }
}

pub fn get_next_version(
    git: &dyn Git,
    config: &RunConfig,
    current_branch: &str,
    pre_release: bool,
) -> Result<VersionData, String> {
    let commit_infos = get_commit_infos(git, &config.tag_prefix)?;
    let last_version_opt = get_last_version(&commit_infos);
    let bumps = get_version_bumps(
        git,
        current_branch,
        last_version_opt.as_ref(),
        &config.commit_message_actions,
        config.when_no_valid_commit_messages,
    )?;
    let next_version = last_version_opt
        .as_ref()
        .map(|lv| lv.version.get_next_version(&bumps, pre_release))
        .unwrap_or_else(|| NextVersion {
            major: 0,
            minor: 1,
            patch: 0,
            pre_release_number: if pre_release { Some(1) } else { None },
        });

    Ok(VersionData {
        major: next_version.major,
        minor: next_version.minor,
        patch: next_version.patch,
        pre_release_number: next_version.pre_release_number,
        commit_count: last_version_opt
            .as_ref()
            .map(|lv| lv.commits_before_head)
            .unwrap_or(commit_infos.len() as u32),
        branch: current_branch.to_string(),
        commit_hash_short: commit_infos
            .first()
            .map(|ci| ci.short_hash.clone())
            .unwrap_or_default(),
        commit_hash_full: commit_infos
            .first()
            .map(|ci| ci.full_hash.clone())
            .unwrap_or_default(),
        date: Local::now().date_naive(),
    })
}

pub fn get_version_bumps(
    git: &dyn Git,
    current_branch: &str,
    last_version: Option<&LastVersion>,
    commit_message_actions: &[CommitMessageAction],
    when_no_valid_commit_messages: IncrementAction,
) -> Result<VersionBumps, String> {
    let log_to_bumps = |log: &str| -> Result<VersionBumps, String> {
        let lines: Vec<&str> = log.lines().collect();
        let log_bumps = calc_bumps(&lines, commit_message_actions, VersionBumps::default());
        if log_bumps.no_valid_commit_messages() {
            get_fallback_version_bumps(when_no_valid_commit_messages, log_bumps)
        } else {
            Ok(log_bumps)
        }
    };

    let bumps = match last_version {
        None => {
            let log = git.full_log(None)?;
            log_to_bumps(&log)?
        }
        Some(LastVersion {
            commits_before_head: 0,
            ..
        }) => VersionBumps::none(),
        Some(lv) => {
            let log = git.full_log(Some(&lv.commit_hash))?;
            log_to_bumps(&log)?
        }
    };

    let release_branch = Regex::new(r"^(hotfix|rel|release)[-/](\d+\.\d+\.\d+)$").unwrap();
    let branch_name_override = release_branch
        .captures(current_branch)
        .and_then(|caps| Version::parse_tag(&caps[2], ""));

    Ok(bumps.with_branch_name_override(branch_name_override))
}

pub fn get_fallback_version_bumps(
    when_no_valid_commit_messages: IncrementAction,
    log_bumps: VersionBumps,
) -> Result<VersionBumps, String> {
    match when_no_valid_commit_messages {
        IncrementAction::Fail => {
            Err("No valid commit messages found describing version increment".to_string())
        }
        IncrementAction::NoIncrement => Ok(log_bumps),
        other => Ok(log_bumps.bump(other)),
    }
}

pub fn calc_bumps(
    lines: &[&str],
    commit_message_actions: &[CommitMessageAction],
    mut bumps: VersionBumps,
) -> VersionBumps {
    let override_regex = Regex::new(r"^    next-version: *(\d+\.\d+\.\d+)$").unwrap();

    for &line in lines {
        if line.starts_with("commit") {
            bumps = bumps.bump_commits();
        } else if line.starts_with("    ") {
            if let Some(caps) = override_regex.captures(line) {
                if let Some(ver) = Version::parse_tag(&caps[1], "") {
                    bumps = bumps.with_commit_override(ver);
                }
            }
            if let Some(action) = commit_message_actions.iter().find_map(|cma| {
                Regex::new(&cma.pattern)
                    .ok()
                    .and_then(|re| re.find(line).map(|_| cma.action))
            }) {
                bumps = bumps.bump(action);
            }
        }
    }
    bumps
}
