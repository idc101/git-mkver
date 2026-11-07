use std::env;
use chrono::Datelike;
use crate::app_config::{merge_formats, Format, RunConfig};
use crate::version::VersionData;

#[derive(Debug, Clone)]
pub struct Formatter {
    pub formats: Vec<Format>,
}

impl Formatter {
    pub fn new(formats: Vec<Format>) -> Self {
        Self { formats }
    }

    pub fn from_version(version: &VersionData, run_config: &RunConfig, pre_release: bool) -> Self {
        let version_format = match (pre_release, run_config.include_build_meta_data) {
            (false, false) => "{Version}",
            (true, false) => "{VersionPreRelease}",
            (false, true) => "{VersionBuildMetaData}",
            (true, true) => "{VersionPreReleaseBuildMetaData}",
        };

        let mut built_in_formats = vec![
            Format::new("Version", "{Major}.{Minor}.{Patch}"),
            Format::new("VersionPreRelease", "{Version}-{PreRelease}"),
            Format::new("VersionBuildMetaData", "{Version}+{BuildMetaData}"),
            Format::new(
                "VersionPreReleaseBuildMetaData",
                "{Version}-{PreRelease}+{BuildMetaData}",
            ),
            Format::new("BuildMetaData", &run_config.build_meta_data_format),
            Format::new("Next", version_format),
            Format::new("Tag", "{TagPrefix}{Next}"),
            Format::new("TagMessage", &run_config.tag_message_format),
            Format::new("PreRelease", &run_config.pre_release_format),
            Format::new(
                "PreReleaseNumber",
                version
                    .pre_release_number
                    .map(|n| n.to_string())
                    .unwrap_or_default(),
            ),
            Format::new("Major", version.major.to_string()),
            Format::new("Minor", version.minor.to_string()),
            Format::new("Patch", version.patch.to_string()),
            Format::new("Branch", branch_name_to_variable(&version.branch)),
            Format::new("ShortHash", &version.commit_hash_short),
            Format::new("FullHash", &version.commit_hash_full),
            Format::new("CommitsSinceTag", version.commit_count.to_string()),
            Format::new("Tagged?", (version.commit_count == 0).to_string()),
            Format::new("dd", format!("{:02}", version.date.day())),
            Format::new("mm", format!("{:02}", version.date.month())),
            Format::new("yyyy", version.date.year().to_string()),
            Format::new("Tag?", run_config.tag.to_string()),
            Format::new("TagPrefix", &run_config.tag_prefix),
        ];

        built_in_formats.extend(env_variables());
        built_in_formats.extend(azure_dev_ops_variables());

        Formatter::new(merge_formats(run_config.formats.clone(), built_in_formats))
    }

    pub fn format(&self, input: &str) -> String {
        self.format_recursive(input, 0)
    }

    fn format_recursive(&self, input: &str, count: usize) -> String {
        if count > 100 {
            // likely an infinite loop
            input.to_string()
        } else {
            let mut sorted_formats = self.formats.clone();
            sorted_formats.sort_by(|a, b| b.name.len().cmp(&a.name.len()));
            let result = sorted_formats.iter().fold(input.to_string(), |s, v| {
                s.replace(&format!("{{{}}}", v.name), &v.format)
            });
            if result == input {
                result
            } else {
                self.format_recursive(&result, count + 1)
            }
        }
    }
}

pub fn azure_dev_ops_variables() -> Vec<Format> {
    env_variable("SYSTEM_PULLREQUEST_SOURCEBRANCH", "AzurePrSourceBranch")
        .map(|mut f| {
            f.format = branch_name_to_variable(&f.format);
            f
        })
        .into_iter()
        .collect()
}

pub fn env_variables() -> Vec<Format> {
    env::vars()
        .map(|(name, value)| Format::new(format!("env.{}", name), value))
        .collect()
}

pub fn env_variable(name: &str, format_name: &str) -> Option<Format> {
    env::var(name)
        .ok()
        .map(|value| Format::new(format_name, value))
}

pub fn branch_name_to_variable(branch_name: &str) -> String {
    branch_name
        .replace("refs/heads/", "")
        .replace("refs/", "")
        .replace('/', "-")
        .replace('_', "-")
}
