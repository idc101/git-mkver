use std::collections::HashMap;
use std::env;
use std::path::Path;
use regex::Regex;
use serde::{Deserialize, Serialize};
use crate::version::{IncrementAction, VersionMode};

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct Format {
    pub name: String,
    pub format: String,
}

impl Format {
    pub fn new(name: impl Into<String>, format: impl Into<String>) -> Self {
        Self {
            name: name.into(),
            format: format.into(),
        }
    }
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct Replacement {
    pub find: String,
    pub replace: String,
}

impl Replacement {
    pub fn new(find: impl Into<String>, replace: impl Into<String>) -> Self {
        Self {
            find: find.into(),
            replace: replace.into(),
        }
    }
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PatchConfig {
    pub name: String,
    pub file_patterns: Vec<String>,
    pub replacements: Vec<Replacement>,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct CommitMessageAction {
    pub pattern: String,
    pub action: IncrementAction,
}

impl CommitMessageAction {
    pub fn new(pattern: impl Into<String>, action: IncrementAction) -> Self {
        Self {
            pattern: pattern.into(),
            action,
        }
    }
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AppConfig {
    #[serde(default = "default_mode")]
    pub mode: VersionMode,
    pub tag_prefix: Option<String>,
    pub defaults: Option<BranchConfigDefaults>,
    pub branches: Option<Vec<BranchConfig>>,
    pub patches: Option<Vec<PatchConfig>>,
    pub commit_message_actions: Option<Vec<CommitMessageAction>>,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct RunConfig {
    pub tag: bool,
    pub tag_prefix: String,
    pub tag_message_format: String,
    pub pre_release_format: String,
    pub build_meta_data_format: String,
    pub include_build_meta_data: bool,
    pub commit_message_actions: Vec<CommitMessageAction>,
    pub when_no_valid_commit_messages: IncrementAction,
    pub formats: Vec<Format>,
    pub patches: Vec<PatchConfig>,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct BranchConfigDefaults {
    #[serde(default = "default_tag")]
    pub tag: bool,
    #[serde(default = "default_tag_message_format")]
    pub tag_message_format: String,
    #[serde(default = "default_pre_release_format")]
    pub pre_release_format: String,
    #[serde(default = "default_build_meta_data_format")]
    pub build_meta_data_format: String,
    #[serde(default = "default_include_build_meta_data")]
    pub include_build_meta_data: bool,
    #[serde(default = "default_when_no_valid_commit_messages")]
    pub when_no_valid_commit_messages: IncrementAction,
    #[serde(default = "default_formats")]
    pub formats: Vec<Format>,
    #[serde(default = "default_patches")]
    pub patches: Vec<String>,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct BranchConfig {
    pub pattern: String,
    pub tag: Option<bool>,
    pub tag_message_format: Option<String>,
    pub pre_release_format: Option<String>,
    pub build_meta_data_format: Option<String>,
    pub include_build_meta_data: Option<bool>,
    pub when_no_valid_commit_messages: Option<IncrementAction>,
    pub formats: Option<Vec<Format>>,
    pub patches: Option<Vec<String>>,
}

fn default_mode() -> VersionMode {
    VersionMode::SemVer
}

fn default_tag() -> bool {
    false
}

fn default_tag_message_format() -> String {
    "release {Tag}".to_string()
}

fn default_pre_release_format() -> String {
    "RC{PreReleaseNumber}".to_string()
}

fn default_build_meta_data_format() -> String {
    "{Branch}.{ShortHash}".to_string()
}

fn default_include_build_meta_data() -> bool {
    true
}

fn default_when_no_valid_commit_messages() -> IncrementAction {
    IncrementAction::IncrementPatch
}

fn default_formats() -> Vec<Format> {
    Vec::new()
}

fn default_patches() -> Vec<String> {
    Vec::new()
}

pub fn validate_pre_release_format(value: &str) -> Result<(), String> {
    if value.contains("{PreReleaseNumber}") && !value.ends_with("{PreReleaseNumber}") {
        Err("preReleaseFormat must end with {PreReleaseNumber}".to_string())
    } else {
        Ok(())
    }
}

pub fn default_default_branch_config() -> BranchConfigDefaults {
    BranchConfigDefaults {
        tag: default_tag(),
        tag_message_format: default_tag_message_format(),
        pre_release_format: default_pre_release_format(),
        build_meta_data_format: default_build_meta_data_format(),
        include_build_meta_data: default_include_build_meta_data(),
        when_no_valid_commit_messages: default_when_no_valid_commit_messages(),
        formats: default_formats(),
        patches: default_patches(),
    }
}

pub fn default_branch_configs() -> Vec<BranchConfig> {
    vec![
        BranchConfig {
            pattern: "master".to_string(),
            tag: Some(true),
            tag_message_format: None,
            pre_release_format: None,
            build_meta_data_format: None,
            include_build_meta_data: Some(false),
            when_no_valid_commit_messages: None,
            formats: None,
            patches: None,
        },
        BranchConfig {
            pattern: "main".to_string(),
            tag: Some(true),
            tag_message_format: None,
            pre_release_format: None,
            build_meta_data_format: None,
            include_build_meta_data: Some(false),
            when_no_valid_commit_messages: None,
            formats: None,
            patches: None,
        },
        BranchConfig {
            pattern: "rel[/-].*".to_string(),
            tag: Some(true),
            tag_message_format: None,
            pre_release_format: None,
            build_meta_data_format: None,
            include_build_meta_data: Some(false),
            when_no_valid_commit_messages: None,
            formats: None,
            patches: None,
        },
        BranchConfig {
            pattern: "release[/-].*".to_string(),
            tag: Some(true),
            tag_message_format: None,
            pre_release_format: None,
            build_meta_data_format: None,
            include_build_meta_data: Some(false),
            when_no_valid_commit_messages: None,
            formats: None,
            patches: None,
        },
        BranchConfig {
            pattern: "hotfix[/-].*".to_string(),
            tag: Some(true),
            tag_message_format: None,
            pre_release_format: None,
            build_meta_data_format: None,
            include_build_meta_data: Some(false),
            when_no_valid_commit_messages: None,
            formats: None,
            patches: None,
        },
    ]
}

pub fn default_commit_message_actions() -> Vec<CommitMessageAction> {
    vec![
        // Breaking changes (major)
        CommitMessageAction::new("BREAKING CHANGE", IncrementAction::IncrementMajor),
        CommitMessageAction::new(r"major(\(.+\))?!:", IncrementAction::IncrementMajor),
        CommitMessageAction::new(r"minor(\(.+\))?!:", IncrementAction::IncrementMajor),
        CommitMessageAction::new(r"patch(\(.+\))?!:", IncrementAction::IncrementMajor),
        CommitMessageAction::new(r"feature(\(.+\))?!:", IncrementAction::IncrementMajor),
        CommitMessageAction::new(r"feat(\(.+\))?!:", IncrementAction::IncrementMajor),
        CommitMessageAction::new(r"fix(\(.+\))?!:", IncrementAction::IncrementMajor),
        // The rest of the conventional commits
        CommitMessageAction::new(
            r"(build|ci|chore|docs|perf|refactor|revert|style|test)(\(.+\))?!:",
            IncrementAction::IncrementMajor,
        ),
        CommitMessageAction::new(r"major(\(.+\))?:", IncrementAction::IncrementMajor),
        CommitMessageAction::new(r"minor(\(.+\))?:", IncrementAction::IncrementMinor),
        CommitMessageAction::new(r"patch(\(.+\))?:", IncrementAction::IncrementPatch),
        CommitMessageAction::new(r"feature(\(.+\))?:", IncrementAction::IncrementMinor),
        CommitMessageAction::new(r"feat(\(.+\))?:", IncrementAction::IncrementMinor),
        CommitMessageAction::new(r"fix(\(.+\))?:", IncrementAction::IncrementPatch),
        // The rest of the conventional commits
        CommitMessageAction::new(
            r"(build|ci|chore|docs|perf|refactor|revert|style|test)(\(.+\))?:",
            IncrementAction::NoIncrement,
        ),
    ]
}

fn matches_full(pattern: &str, text: &str) -> bool {
    let anchored = format!("^(?:{})$", pattern);
    Regex::new(&anchored)
        .map(|re| re.is_match(text))
        .unwrap_or(false)
}

pub fn get_run_config(config_file: Option<&str>, current_branch: &str) -> Result<RunConfig, String> {
    let app_config = get_app_config(config_file)?;
    let defaults = app_config
        .as_ref()
        .and_then(|c| c.defaults.clone())
        .unwrap_or_else(default_default_branch_config);
    let branches = app_config
        .as_ref()
        .and_then(|c| c.branches.clone())
        .unwrap_or_else(default_branch_configs);
    let branch_config = branches
        .into_iter()
        .find(|bc| matches_full(&bc.pattern, current_branch));
    let patch_names = branch_config
        .as_ref()
        .and_then(|bc| bc.patches.clone())
        .unwrap_or_else(|| defaults.patches.clone());
    let all_patches = app_config
        .as_ref()
        .and_then(|c| c.patches.clone())
        .unwrap_or_default();
    let patch_configs = get_patch_configs(&all_patches, &patch_names)?;

    let pre_release_format = branch_config
        .as_ref()
        .and_then(|bc| bc.pre_release_format.clone())
        .unwrap_or_else(|| defaults.pre_release_format.clone());
    validate_pre_release_format(&pre_release_format)?;

    Ok(RunConfig {
        tag: branch_config
            .as_ref()
            .and_then(|bc| bc.tag)
            .unwrap_or(defaults.tag),
        tag_prefix: app_config
            .as_ref()
            .and_then(|c| c.tag_prefix.clone())
            .unwrap_or_else(|| "v".to_string()),
        tag_message_format: branch_config
            .as_ref()
            .and_then(|bc| bc.tag_message_format.clone())
            .unwrap_or(defaults.tag_message_format),
        pre_release_format,
        build_meta_data_format: branch_config
            .as_ref()
            .and_then(|bc| bc.build_meta_data_format.clone())
            .unwrap_or(defaults.build_meta_data_format),
        include_build_meta_data: branch_config
            .as_ref()
            .and_then(|bc| bc.include_build_meta_data)
            .unwrap_or(defaults.include_build_meta_data),
        commit_message_actions: merge_commit_message_actions(
            default_commit_message_actions(),
            app_config
                .as_ref()
                .and_then(|c| c.commit_message_actions.clone())
                .unwrap_or_default(),
        ),
        when_no_valid_commit_messages: branch_config
            .as_ref()
            .and_then(|bc| bc.when_no_valid_commit_messages)
            .unwrap_or(defaults.when_no_valid_commit_messages),
        formats: merge_formats(
            defaults.formats,
            branch_config
                .as_ref()
                .and_then(|bc| bc.formats.clone())
                .unwrap_or_default(),
        ),
        patches: patch_configs,
    })
}

pub fn merge_formats(start_list: Vec<Format>, overrides: Vec<Format>) -> Vec<Format> {
    merge(start_list, overrides, |f| f.name.clone())
}

pub fn merge_commit_message_actions(
    start_list: Vec<CommitMessageAction>,
    overrides: Vec<CommitMessageAction>,
) -> Vec<CommitMessageAction> {
    merge(start_list, overrides, |cma| cma.pattern.clone())
}

pub fn merge<T, F>(start_list: Vec<T>, overrides: Vec<T>, get_name: F) -> Vec<T>
where
    F: Fn(&T) -> String,
{
    let mut map: HashMap<String, T> = start_list
        .into_iter()
        .map(|it| (get_name(&it), it))
        .collect();
    for item in overrides {
        map.insert(get_name(&item), item);
    }
    let mut vec: Vec<T> = map.into_values().collect();
    vec.sort_by(|a, b| get_name(a).cmp(&get_name(b)));
    vec
}

pub fn get_patch_configs(
    patches: &[PatchConfig],
    patch_names: &[String],
) -> Result<Vec<PatchConfig>, String> {
    let all_patch_configs: HashMap<&str, &PatchConfig> =
        patches.iter().map(|it| (it.name.as_str(), it)).collect();
    let mut result = Vec::with_capacity(patch_names.len());
    for name in patch_names {
        match all_patch_configs.get(name.as_str()) {
            Some(p) => result.push((*p).clone()),
            None => return Err(format!("Can't find patch config named {}", name)),
        }
    }
    Ok(result)
}

pub fn get_app_config(config_file: Option<&str>) -> Result<Option<AppConfig>, String> {
    let file_opt = if let Some(cf) = config_file {
        if Path::new(cf).exists() {
            Some(cf.to_string())
        } else {
            return Err(format!("--config {} does not exist", cf));
        }
    } else if let Ok(cf) = env::var("GITMKVER_CONFIG") {
        if Path::new(&cf).exists() {
            Some(cf)
        } else {
            return Err(format!("GITMKVER_CONFIG {} does not exist", cf));
        }
    } else if Path::new("mkver.conf").exists() {
        Some("mkver.conf".to_string())
    } else {
        None
    };

    match file_opt {
        Some(file) => try_load_app_config(&file).map(Some),
        None => Ok(None),
    }
}

pub fn try_load_app_config(file: &str) -> Result<AppConfig, String> {
    let config: AppConfig = hocon::HoconLoader::new()
        .load_file(file)
        .map_err(|e| format!("Unable to parse config: {}", e))?
        .resolve()
        .map_err(|e| format!("Unable to parse config: {}", e))?;

    if let Some(ref defaults) = config.defaults {
        validate_pre_release_format(&defaults.pre_release_format)?;
    }
    if let Some(ref branches) = config.branches {
        for b in branches {
            if let Some(ref prf) = b.pre_release_format {
                validate_pre_release_format(prf)?;
            }
        }
    }

    Ok(config)
}

impl RunConfig {
    pub fn to_hocon_string(&self) -> String {
        let mut out = String::new();
        out.push_str(&format!("buildMetaDataFormat=\"{}\"\n", escape_hocon(&self.build_meta_data_format)));
        out.push_str("commitMessageActions=[\n");
        for (i, cma) in self.commit_message_actions.iter().enumerate() {
            out.push_str("    {\n");
            out.push_str(&format!("        action={}\n", cma.action));
            out.push_str(&format!("        pattern=\"{}\"\n", escape_hocon(&cma.pattern)));
            out.push_str("    }");
            if i + 1 < self.commit_message_actions.len() {
                out.push(',');
            }
            out.push('\n');
        }
        out.push_str("]\n");
        out.push_str("formats=[\n");
        for (i, fmt) in self.formats.iter().enumerate() {
            out.push_str("    {\n");
            out.push_str(&format!("        format=\"{}\"\n", escape_hocon(&fmt.format)));
            out.push_str(&format!("        name=\"{}\"\n", escape_hocon(&fmt.name)));
            out.push_str("    }");
            if i + 1 < self.formats.len() {
                out.push(',');
            }
            out.push('\n');
        }
        out.push_str("]\n");
        out.push_str(&format!("includeBuildMetaData={}\n", self.include_build_meta_data));
        out.push_str("patches=[\n");
        for (i, p) in self.patches.iter().enumerate() {
            out.push_str("    {\n");
            out.push_str("        filePatterns=[\n");
            for (j, fp) in p.file_patterns.iter().enumerate() {
                out.push_str(&format!("            \"{}\"", escape_hocon(fp)));
                if j + 1 < p.file_patterns.len() {
                    out.push(',');
                }
                out.push('\n');
            }
            out.push_str("        ]\n");
            out.push_str(&format!("        name=\"{}\"\n", escape_hocon(&p.name)));
            out.push_str("        replacements=[\n");
            for (j, r) in p.replacements.iter().enumerate() {
                out.push_str("            {\n");
                out.push_str(&format!("                find=\"{}\"\n", escape_hocon(&r.find)));
                out.push_str(&format!("                replace=\"{}\"\n", escape_hocon(&r.replace)));
                out.push_str("            }");
                if j + 1 < p.replacements.len() {
                    out.push(',');
                }
                out.push('\n');
            }
            out.push_str("        ]\n");
            out.push_str("    }");
            if i + 1 < self.patches.len() {
                out.push(',');
            }
            out.push('\n');
        }
        out.push_str("]\n");
        out.push_str(&format!("preReleaseFormat=\"{}\"\n", escape_hocon(&self.pre_release_format)));
        out.push_str(&format!("tag={}\n", self.tag));
        out.push_str(&format!("tagMessageFormat=\"{}\"\n", escape_hocon(&self.tag_message_format)));
        out.push_str(&format!("tagPrefix=\"{}\"\n", escape_hocon(&self.tag_prefix)));
        out.push_str(&format!("whenNoValidCommitMessages={}\n", self.when_no_valid_commit_messages));
        out
    }
}

fn escape_hocon(s: &str) -> String {
    s.replace('\\', "\\\\").replace('"', "\\\"")
}
