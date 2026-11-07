use std::cmp::Ordering;
use chrono::NaiveDate;
use regex::Regex;
use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub enum VersionMode {
    SemVer,
    YearMonth,
}

impl std::fmt::Display for VersionMode {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            VersionMode::SemVer => write!(f, "SemVer"),
            VersionMode::YearMonth => write!(f, "YearMonth"),
        }
    }
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub enum IncrementAction {
    Fail,
    IncrementMajor,
    IncrementMinor,
    IncrementPatch,
    NoIncrement,
}

impl std::fmt::Display for IncrementAction {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            IncrementAction::Fail => write!(f, "Fail"),
            IncrementAction::IncrementMajor => write!(f, "IncrementMajor"),
            IncrementAction::IncrementMinor => write!(f, "IncrementMinor"),
            IncrementAction::IncrementPatch => write!(f, "IncrementPatch"),
            IncrementAction::NoIncrement => write!(f, "NoIncrement"),
        }
    }
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Version {
    pub major: u32,
    pub minor: u32,
    pub patch: u32,
    pub pre_release: Option<String>,
    pub build_meta_data: Option<String>,
}

pub const VERSION_ONLY_REGEX: &str = r"(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)";
pub const PRERELEASE_REGEX: &str = r"(?:-((?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\.(?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?";
pub const METADATA_REGEX: &str = r"(?:\+([0-9a-zA-Z-]+(?:\.[0-9a-zA-Z-]+)*))?";
pub const VERSION_FULL_REGEX: &str = r"(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)(?:-((?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\.(?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\+([0-9a-zA-Z-]+(?:\.[0-9a-zA-Z-]+)*))?";

impl Version {
    pub fn new(
        major: u32,
        minor: u32,
        patch: u32,
        pre_release: Option<String>,
        build_meta_data: Option<String>,
    ) -> Self {
        Self {
            major,
            minor,
            patch,
            pre_release,
            build_meta_data,
        }
    }

    pub fn simple(major: u32, minor: u32, patch: u32) -> Self {
        Self::new(major, minor, patch, None, None)
    }

    pub fn get_next_version(&self, bumps: &VersionBumps, bump_pre_release: bool) -> NextVersion {
        match (&self.pre_release, bump_pre_release) {
            (Some(_), false) => {
                // Generating a non-pre-release version after a pre-release is always the version without pre-release
                // ... unless an override has been specified.
                let mut no_bumps = bumps.clone();
                no_bumps.major = false;
                no_bumps.minor = false;
                no_bumps.patch = false;
                self.bump(&no_bumps, None)
            }
            (Some(_), true) => {
                // Last version was a pre-release and next version is also a pre-release
                let next_pre_release_number = match self.pre_release_number() {
                    Some(last_pre_release_number) => Some(last_pre_release_number + 1),
                    None => Some(1),
                };
                if bumps
                    .branch_name_override
                    .as_ref()
                    .map_or(false, |v| !self.equals_version_core(v))
                    || bumps
                        .commit_override
                        .as_ref()
                        .map_or(false, |v| !self.equals_version_core(v))
                {
                    // there is an override to a different version - reset the pre-release number
                    self.bump(bumps, Some(1))
                } else {
                    // No override
                    NextVersion {
                        major: self.major,
                        minor: self.minor,
                        patch: self.patch,
                        pre_release_number: next_pre_release_number,
                    }
                }
            }
            (None, true) => {
                // Last version was not a pre-release but this version is
                // Bump the version number and add in the pre-release
                let next_pre_release_number = Some(1);
                self.bump(bumps, next_pre_release_number)
            }
            (None, false) => {
                // Not a pre-release previously and next version is not a pre-release either
                self.bump(bumps, None)
            }
        }
    }

    pub fn pre_release_number(&self) -> Option<u32> {
        let re = Regex::new(r"^.*(\d+)$").unwrap();
        match &self.pre_release {
            Some(pr) => {
                if let Some(caps) = re.captures(pr) {
                    Some(caps[1].parse::<u32>().unwrap_or(0))
                } else {
                    Some(0)
                }
            }
            None => None,
        }
    }

    pub fn bump(&self, bumps: &VersionBumps, new_pre_release: Option<u32>) -> NextVersion {
        if let Some(commit_override) = &bumps.commit_override {
            NextVersion {
                major: commit_override.major,
                minor: commit_override.minor,
                patch: commit_override.patch,
                pre_release_number: new_pre_release,
            }
        } else if let Some(branch_name_override) = &bumps.branch_name_override {
            NextVersion {
                major: branch_name_override.major,
                minor: branch_name_override.minor,
                patch: branch_name_override.patch,
                pre_release_number: new_pre_release,
            }
        } else if bumps.major {
            NextVersion {
                major: self.major + 1,
                minor: 0,
                patch: 0,
                pre_release_number: new_pre_release,
            }
        } else if bumps.minor {
            NextVersion {
                major: self.major,
                minor: self.minor + 1,
                patch: 0,
                pre_release_number: new_pre_release,
            }
        } else if bumps.patch {
            NextVersion {
                major: self.major,
                minor: self.minor,
                patch: self.patch + 1,
                pre_release_number: new_pre_release,
            }
        } else {
            NextVersion {
                major: self.major,
                minor: self.minor,
                patch: self.patch,
                pre_release_number: new_pre_release,
            }
        }
    }

    pub fn equals_version_core(&self, other: &Version) -> bool {
        self.major == other.major && self.minor == other.minor && self.patch == other.patch
    }

    pub fn parse_tag(input: &str, prefix: &str) -> Option<Version> {
        let pattern = format!("^{}{}$", prefix, VERSION_FULL_REGEX);
        let re = Regex::new(&pattern).ok()?;
        let caps = re.captures(input)?;
        let major = caps.get(1)?.as_str().parse::<u32>().ok()?;
        let minor = caps.get(2)?.as_str().parse::<u32>().ok()?;
        let patch = caps.get(3)?.as_str().parse::<u32>().ok()?;
        let pre_release = caps.get(4).map(|m| m.as_str().to_string());
        let build_meta_data = caps.get(5).map(|m| m.as_str().to_string());
        Some(Version {
            major,
            minor,
            patch,
            pre_release,
            build_meta_data,
        })
    }
}

impl PartialOrd for Version {
    fn partial_cmp(&self, other: &Self) -> Option<Ordering> {
        Some(self.cmp(other))
    }
}

impl Ord for Version {
    fn cmp(&self, other: &Self) -> Ordering {
        match (
            self.major.cmp(&other.major),
            self.minor.cmp(&other.minor),
            self.patch.cmp(&other.patch),
        ) {
            (Ordering::Equal, Ordering::Equal, Ordering::Equal) => {
                match (self.pre_release_number(), other.pre_release_number()) {
                    (None, None) => Ordering::Equal,
                    (Some(_), None) => Ordering::Less,
                    (None, Some(_)) => Ordering::Greater,
                    (Some(l_pre_num), Some(r_pre_num)) => l_pre_num.cmp(&r_pre_num),
                }
            }
            (Ordering::Equal, Ordering::Equal, ord) => ord,
            (Ordering::Equal, ord, _) => ord,
            (ord, _, _) => ord,
        }
    }
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct NextVersion {
    pub major: u32,
    pub minor: u32,
    pub patch: u32,
    pub pre_release_number: Option<u32>,
}

impl NextVersion {
    pub fn new(major: u32, minor: u32, patch: u32, pre_release_number: Option<u32>) -> Self {
        Self {
            major,
            minor,
            patch,
            pre_release_number,
        }
    }
}

#[derive(Debug, Clone, PartialEq, Eq, Default)]
pub struct VersionBumps {
    pub major: bool,
    pub minor: bool,
    pub patch: bool,
    pub commit_count: u32,
    pub branch_name_override: Option<Version>,
    pub commit_override: Option<Version>,
}

impl VersionBumps {
    pub fn none() -> Self {
        Self::default()
    }

    pub fn with_branch_name_override(mut self, version: Option<Version>) -> Self {
        self.branch_name_override = version;
        self
    }

    pub fn with_commit_override(mut self, version: Version) -> Self {
        self.commit_override = Some(version);
        self
    }

    pub fn bump(mut self, increment_action: IncrementAction) -> Self {
        match increment_action {
            IncrementAction::IncrementMajor => self.major = true,
            IncrementAction::IncrementMinor => self.minor = true,
            IncrementAction::IncrementPatch => self.patch = true,
            _ => {}
        }
        self
    }

    pub fn bump_commits(mut self) -> Self {
        self.commit_count += 1;
        self
    }

    pub fn no_valid_commit_messages(&self) -> bool {
        !self.major && !self.minor && !self.patch
    }
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct VersionData {
    pub major: u32,
    pub minor: u32,
    pub patch: u32,
    pub pre_release_number: Option<u32>,
    pub commit_count: u32,
    pub branch: String,
    pub commit_hash_short: String,
    pub commit_hash_full: String,
    pub date: NaiveDate,
}
