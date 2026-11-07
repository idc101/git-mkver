use std::collections::HashSet;
use std::path::{Path, PathBuf};
use git_mkver::app_config::{
    default_commit_message_actions, get_run_config, merge_formats, try_load_app_config, Format,
    RunConfig,
};
use git_mkver::files;
use git_mkver::formatter::{branch_name_to_variable, Formatter};
use git_mkver::git::{exec, Git, LiveGit};
use git_mkver::mkver::{
    calc_bumps, format_version, get_commit_infos, get_fallback_version_bumps, get_last_version,
    CommitInfo, LastVersion,
};
use git_mkver::version::{IncrementAction, NextVersion, Version, VersionBumps, VersionData};
use git_mkver::{main_impl, Console};
use chrono::Local;

// ============================================================================
// VersionSpec
// ============================================================================

#[test]
fn version_get_next_version_should_return_correctly() {
    let vb = VersionBumps {
        major: true,
        minor: true,
        patch: true,
        commit_count: 0,
        branch_name_override: None,
        commit_override: None,
    };
    assert_eq!(
        Version::new(1, 2, 3, Some("RC4".to_string()), None).get_next_version(&vb, false),
        NextVersion::new(1, 2, 3, None)
    );
    assert_eq!(
        Version::new(1, 2, 3, Some("RC4".to_string()), None).get_next_version(&vb, true),
        NextVersion::new(1, 2, 3, Some(5))
    );
    assert_eq!(
        Version::new(1, 2, 3, Some("RC".to_string()), None).get_next_version(&vb, true),
        NextVersion::new(1, 2, 3, Some(1))
    );
    assert_eq!(
        Version::new(1, 2, 3, None, None).get_next_version(&vb, true),
        NextVersion::new(2, 0, 0, Some(1))
    );
    assert_eq!(
        Version::new(1, 2, 3, None, None).get_next_version(&vb, false),
        NextVersion::new(2, 0, 0, None)
    );
}

#[test]
fn version_get_next_version_with_commit_override() {
    let vb = VersionBumps {
        major: true,
        minor: true,
        patch: true,
        commit_count: 0,
        branch_name_override: None,
        commit_override: Some(Version::simple(3, 4, 5)),
    };
    assert_eq!(
        Version::new(1, 2, 3, Some("RC4".to_string()), None).get_next_version(&vb, false),
        NextVersion::new(3, 4, 5, None)
    );
    assert_eq!(
        Version::new(1, 2, 3, Some("RC4".to_string()), None).get_next_version(&vb, true),
        NextVersion::new(3, 4, 5, Some(1))
    );
    assert_eq!(
        Version::new(1, 2, 3, Some("RC".to_string()), None).get_next_version(&vb, true),
        NextVersion::new(3, 4, 5, Some(1))
    );
    assert_eq!(
        Version::new(1, 2, 3, None, None).get_next_version(&vb, true),
        NextVersion::new(3, 4, 5, Some(1))
    );
    assert_eq!(
        Version::new(1, 2, 3, None, None).get_next_version(&vb, false),
        NextVersion::new(3, 4, 5, None)
    );
}

#[test]
fn version_get_next_version_with_branch_override() {
    let vb = VersionBumps {
        major: true,
        minor: true,
        patch: true,
        commit_count: 0,
        branch_name_override: Some(Version::simple(3, 4, 5)),
        commit_override: None,
    };
    assert_eq!(
        Version::new(1, 2, 3, Some("RC4".to_string()), None).get_next_version(&vb, false),
        NextVersion::new(3, 4, 5, None)
    );
    assert_eq!(
        Version::new(1, 2, 3, Some("RC4".to_string()), None).get_next_version(&vb, true),
        NextVersion::new(3, 4, 5, Some(1))
    );
    assert_eq!(
        Version::new(1, 2, 3, Some("RC".to_string()), None).get_next_version(&vb, true),
        NextVersion::new(3, 4, 5, Some(1))
    );
    assert_eq!(
        Version::new(1, 2, 3, None, None).get_next_version(&vb, true),
        NextVersion::new(3, 4, 5, Some(1))
    );
    assert_eq!(
        Version::new(1, 2, 3, None, None).get_next_version(&vb, false),
        NextVersion::new(3, 4, 5, None)
    );
}

#[test]
fn version_bump_should_bump_correctly() {
    let v100 = Version::simple(1, 0, 0);
    assert_eq!(
        v100.bump(
            &VersionBumps {
                major: true,
                minor: true,
                patch: true,
                commit_count: 0,
                branch_name_override: Some(Version::simple(3, 0, 0)),
                commit_override: None
            },
            Some(6)
        ),
        NextVersion::new(3, 0, 0, Some(6))
    );
    assert_eq!(
        v100.bump(
            &VersionBumps {
                major: true,
                minor: true,
                patch: true,
                commit_count: 0,
                branch_name_override: None,
                commit_override: Some(Version::simple(4, 0, 0))
            },
            Some(6)
        ),
        NextVersion::new(4, 0, 0, Some(6))
    );
    assert_eq!(
        v100.bump(
            &VersionBumps {
                major: true,
                minor: true,
                patch: true,
                commit_count: 0,
                branch_name_override: Some(Version::simple(3, 0, 0)),
                commit_override: Some(Version::simple(4, 0, 0))
            },
            Some(6)
        ),
        NextVersion::new(4, 0, 0, Some(6))
    );
    assert_eq!(
        v100.bump(
            &VersionBumps {
                major: true,
                minor: true,
                patch: true,
                ..Default::default()
            },
            Some(6)
        ),
        NextVersion::new(2, 0, 0, Some(6))
    );
    assert_eq!(
        v100.bump(
            &VersionBumps {
                major: false,
                minor: true,
                patch: true,
                ..Default::default()
            },
            Some(6)
        ),
        NextVersion::new(1, 1, 0, Some(6))
    );
    assert_eq!(
        v100.bump(
            &VersionBumps {
                major: false,
                minor: false,
                patch: true,
                ..Default::default()
            },
            Some(6)
        ),
        NextVersion::new(1, 0, 1, Some(6))
    );
    assert_eq!(
        v100.bump(&VersionBumps::default(), Some(6)),
        NextVersion::new(1, 0, 0, Some(6))
    );

    let v123 = Version::simple(1, 2, 3);
    assert_eq!(
        v123.bump(
            &VersionBumps {
                major: true,
                minor: true,
                patch: true,
                ..Default::default()
            },
            Some(6)
        ),
        NextVersion::new(2, 0, 0, Some(6))
    );
    assert_eq!(
        v123.bump(
            &VersionBumps {
                major: false,
                minor: true,
                patch: true,
                ..Default::default()
            },
            Some(6)
        ),
        NextVersion::new(1, 3, 0, Some(6))
    );
    assert_eq!(
        v123.bump(
            &VersionBumps {
                major: false,
                minor: false,
                patch: true,
                ..Default::default()
            },
            Some(6)
        ),
        NextVersion::new(1, 2, 4, Some(6))
    );
    assert_eq!(
        v123.bump(&VersionBumps::default(), Some(6)),
        NextVersion::new(1, 2, 3, Some(6))
    );
}

#[test]
fn version_parse_tag_should_parse_correctly() {
    assert_eq!(
        Version::parse_tag("v10.5.3", "v"),
        Some(Version::new(10, 5, 3, None, None))
    );
    assert_eq!(
        Version::parse_tag("v10.5.3-pre", "v"),
        Some(Version::new(10, 5, 3, Some("pre".to_string()), None))
    );
    assert_eq!(
        Version::parse_tag("v10.5.3-pre+build", "v"),
        Some(Version::new(
            10,
            5,
            3,
            Some("pre".to_string()),
            Some("build".to_string())
        ))
    );
    assert_eq!(
        Version::parse_tag("v10.5.3+build", "v"),
        Some(Version::new(10, 5, 3, None, Some("build".to_string())))
    );
    assert_eq!(
        Version::parse_tag("v10.5.3-pre-3+build", "v"),
        Some(Version::new(
            10,
            5,
            3,
            Some("pre-3".to_string()),
            Some("build".to_string())
        ))
    );
    assert_eq!(
        Version::parse_tag("v10.5.3-pre-3+build-1", "v"),
        Some(Version::new(
            10,
            5,
            3,
            Some("pre-3".to_string()),
            Some("build-1".to_string())
        ))
    );
    assert_eq!(
        Version::parse_tag("v10.5.3+build-1", "v"),
        Some(Version::new(10, 5, 3, None, Some("build-1".to_string())))
    );
}

#[test]
fn version_ordering() {
    // less than
    assert!(Version::new(0, 2, 3, None, None) < Version::new(1, 2, 3, None, None));
    assert!(Version::new(1, 1, 3, None, None) < Version::new(1, 2, 3, None, None));
    assert!(Version::new(1, 2, 2, None, None) < Version::new(1, 2, 3, None, None));

    // equal
    assert!(!(Version::new(1, 2, 3, None, None) < Version::new(1, 2, 3, None, None)));

    // greater than
    assert!(!(Version::new(2, 2, 3, None, None) < Version::new(1, 2, 3, None, None)));
    assert!(!(Version::new(1, 3, 3, None, None) < Version::new(1, 2, 3, None, None)));
    assert!(!(Version::new(1, 2, 4, None, None) < Version::new(1, 2, 3, None, None)));

    // pre-release vs non pre-release less than
    assert!(Version::new(1, 2, 3, Some("RC1".to_string()), None) < Version::new(1, 2, 3, None, None));
    assert!(!(Version::new(1, 2, 3, None, None) < Version::new(1, 2, 3, Some("RC1".to_string()), None)));

    // pre-release vs pre-release less than
    assert!(
        Version::new(1, 2, 3, Some("RC1".to_string()), None)
            < Version::new(1, 2, 3, Some("RC2".to_string()), None)
    );
    assert!(
        !(Version::new(1, 2, 3, Some("RC2".to_string()), None)
            < Version::new(1, 2, 3, Some("RC1".to_string()), None))
    );
}

// ============================================================================
// FormatterSpec
// ============================================================================

#[test]
fn formatter_should_replace_variables() {
    let formatter = Formatter::new(vec![
        Format::new("a", "1"),
        Format::new("a3", "3"),
        Format::new("b", "2"),
        Format::new("c", "{a}.{b}"),
        Format::new("r1", "{r2}"),
    ]);
    assert_eq!(formatter.format("hello"), "hello");
    assert_eq!(formatter.format("{a}"), "1");
    assert_eq!(formatter.format("{a}-{c}"), "1-1.2");
    assert_eq!(formatter.format("{r1}"), "{r2}");
    assert_eq!(formatter.format("{a3}"), "3");
}

#[test]
fn formatter_branch_name_to_variable_should_sanitize_name() {
    assert_eq!(branch_name_to_variable("refs/heads/feat/f1"), "feat-f1");
}

#[test]
fn formatter_should_format_default_variables() {
    let version_data = VersionData {
        major: 1,
        minor: 2,
        patch: 3,
        pre_release_number: None,
        commit_count: 4,
        branch: "feature/f1".to_string(),
        commit_hash_short: "abcd".to_string(),
        commit_hash_full: "abcdefg".to_string(),
        date: Local::now().date_naive(),
    };
    let run_config = RunConfig {
        tag: true,
        tag_prefix: "v".to_string(),
        tag_message_format: "release {Version}".to_string(),
        pre_release_format: "RC".to_string(),
        build_meta_data_format: "{Branch}".to_string(),
        include_build_meta_data: false,
        commit_message_actions: vec![],
        when_no_valid_commit_messages: IncrementAction::IncrementMinor,
        formats: vec![],
        patches: vec![],
    };
    let formatter = Formatter::from_version(&version_data, &run_config, false);
    assert_eq!(formatter.format("{Major}"), "1");
    assert_eq!(formatter.format("{Minor}"), "2");
    assert_eq!(formatter.format("{Patch}"), "3");
    assert_eq!(formatter.format("{Branch}"), "feature-f1");
}

#[test]
fn formatter_next_variants() {
    let version_data = VersionData {
        major: 1,
        minor: 2,
        patch: 3,
        pre_release_number: Some(3),
        commit_count: 4,
        branch: "feature/f1".to_string(),
        commit_hash_short: "abcd".to_string(),
        commit_hash_full: "abcdefg".to_string(),
        date: Local::now().date_naive(),
    };

    // Next pre-release=false buildmetadata=false
    let rc1 = RunConfig {
        tag: true,
        tag_prefix: "v".to_string(),
        tag_message_format: "release {Version}".to_string(),
        pre_release_format: "RC".to_string(),
        build_meta_data_format: "{Branch}".to_string(),
        include_build_meta_data: false,
        commit_message_actions: vec![],
        when_no_valid_commit_messages: IncrementAction::IncrementMinor,
        formats: vec![],
        patches: vec![],
    };
    assert_eq!(
        Formatter::from_version(&version_data, &rc1, false).format("{Next}"),
        "1.2.3"
    );

    // Next pre-release=false buildmetadata=true
    let mut rc2 = rc1.clone();
    rc2.include_build_meta_data = true;
    assert_eq!(
        Formatter::from_version(&version_data, &rc2, false).format("{Next}"),
        "1.2.3+feature-f1"
    );

    // Next pre-release=true buildmetadata=false
    let mut rc3 = rc1.clone();
    rc3.pre_release_format = "RC{PreReleaseNumber}".to_string();
    assert_eq!(
        Formatter::from_version(&version_data, &rc3, true).format("{Next}"),
        "1.2.3-RC3"
    );

    // Next pre-release=true buildmetadata=true
    let mut rc4 = rc3.clone();
    rc4.include_build_meta_data = true;
    assert_eq!(
        Formatter::from_version(&version_data, &rc4, true).format("{Next}"),
        "1.2.3-RC3+feature-f1"
    );
}

#[test]
fn formatter_picks_up_custom_formats() {
    let version_data = VersionData {
        major: 1,
        minor: 2,
        patch: 3,
        pre_release_number: None,
        commit_count: 0,
        branch: "main".to_string(),
        commit_hash_short: "abcd".to_string(),
        commit_hash_full: "abcdefg".to_string(),
        date: Local::now().date_naive(),
    };
    let formats = vec![Format::new("Docker", "docker - {Version}")];
    let run_config = RunConfig {
        tag: true,
        tag_prefix: "v".to_string(),
        tag_message_format: "release {Version}".to_string(),
        pre_release_format: "RC{PreReleaseNumber}".to_string(),
        build_meta_data_format: "{Branch}".to_string(),
        include_build_meta_data: true,
        commit_message_actions: vec![],
        when_no_valid_commit_messages: IncrementAction::IncrementMinor,
        formats,
        patches: vec![],
    };
    let formatter = Formatter::from_version(&version_data, &run_config, false);
    assert_eq!(formatter.format("{Docker}"), "docker - 1.2.3");
}

// ============================================================================
// AppConfigSpec
// ============================================================================

#[test]
fn app_config_get_run_config() {
    let main_cfg = get_run_config(None, "main").unwrap();
    assert!(main_cfg.tag);

    let feat_cfg = get_run_config(None, "feat").unwrap();
    assert!(!feat_cfg.tag);
    assert_eq!(feat_cfg.build_meta_data_format, "{Branch}.{ShortHash}");
}

#[test]
fn app_config_merge_formats() {
    let f1 = Format::new("f1", "v1");
    let f2 = Format::new("f2", "v2");
    let f3 = Format::new("f3", "v3");
    let f1b = Format::new("f1", "v4");
    assert_eq!(
        merge_formats(vec![f1, f3.clone()], vec![f1b.clone(), f2.clone()]),
        vec![f1b, f2, f3]
    );
}

#[test]
fn app_config_load_hocon_files() {
    let ref_cfg = try_load_app_config("src/main/resources/reference.conf");
    if Path::new("src/main/resources/reference.conf").exists() {
        assert!(ref_cfg.is_ok(), "Failed to parse reference.conf: {:?}", ref_cfg);
    }
    if Path::new("git-mkver.conf").exists() {
        let self_cfg = try_load_app_config("git-mkver.conf");
        assert!(self_cfg.is_ok(), "Failed to parse git-mkver.conf: {:?}", self_cfg);
    }
}

// ============================================================================
// FilesSpec
// ============================================================================

#[test]
fn files_glob_tests() {
    let temp = tempfile::tempdir().unwrap();
    let root = temp.path();
    std::fs::write(root.join("build.sbt"), "").unwrap();
    std::fs::create_dir_all(root.join("project")).unwrap();
    std::fs::write(root.join("project").join("plugins.sbt"), "").unwrap();

    let prev_dir = std::env::current_dir().unwrap();
    std::env::set_current_dir(root).unwrap();

    let cwd = Path::new("");
    let res1: Vec<String> = files::glob(cwd, "build.sbt")
        .unwrap()
        .into_iter()
        .map(|p| p.to_string_lossy().to_string())
        .collect();
    assert_eq!(res1, vec!["build.sbt".to_string()]);

    let res2: Vec<String> = files::glob(cwd, "*.sbt")
        .unwrap()
        .into_iter()
        .map(|p| p.to_string_lossy().to_string())
        .collect();
    assert_eq!(res2, vec!["build.sbt".to_string()]);

    let sep = std::path::MAIN_SEPARATOR;
    let res3: HashSet<String> = files::glob(cwd, "**/*.sbt")
        .unwrap()
        .into_iter()
        .map(|p| p.to_string_lossy().to_string())
        .collect();
    assert_eq!(
        res3,
        HashSet::from([format!("project{}plugins.sbt", sep)])
    );

    let res4: HashSet<String> = files::glob(cwd, "***.sbt")
        .unwrap()
        .into_iter()
        .map(|p| p.to_string_lossy().to_string())
        .collect();
    assert_eq!(
        res4,
        HashSet::from([
            format!("project{}plugins.sbt", sep),
            "build.sbt".to_string()
        ])
    );

    std::env::set_current_dir(prev_dir).unwrap();
}

// ============================================================================
// MkVerSpec
// ============================================================================

struct MockGit {
    current_branch: String,
    full_log: String,
    commit_info_log: String,
}

impl Git for MockGit {
    fn current_branch(&self) -> Result<String, String> {
        Ok(self.current_branch.clone())
    }
    fn full_log(&self, _from_ref: Option<&str>) -> Result<String, String> {
        Ok(self.full_log.clone())
    }
    fn commit_info_log(&self) -> Result<String, String> {
        Ok(self.commit_info_log.clone())
    }
    fn tag(&self, _tag: &str, _tag_message: &str) -> Result<(), String> {
        Ok(())
    }
    fn check_git_repo(&self) -> Result<(), String> {
        Ok(())
    }
}

#[test]
fn mkver_calc_bumps_should_parse_correctly() {
    let cmas = default_commit_message_actions();
    assert_eq!(
        calc_bumps(&["    major: change"], &cmas, VersionBumps::default()),
        VersionBumps {
            major: true,
            ..Default::default()
        }
    );
    assert_eq!(
        calc_bumps(
            &["    feat: change", "    BREAKING CHANGE"],
            &cmas,
            VersionBumps::default()
        ),
        VersionBumps {
            major: true,
            minor: true,
            ..Default::default()
        }
    );
    assert_eq!(
        calc_bumps(&["    feat!: change"], &cmas, VersionBumps::default()),
        VersionBumps {
            major: true,
            ..Default::default()
        }
    );
    assert_eq!(
        calc_bumps(&["    fix!: change"], &cmas, VersionBumps::default()),
        VersionBumps {
            major: true,
            ..Default::default()
        }
    );
    assert_eq!(
        calc_bumps(&["    patch!: change"], &cmas, VersionBumps::default()),
        VersionBumps {
            major: true,
            ..Default::default()
        }
    );
    assert_eq!(
        calc_bumps(
            &["    major(a component)!: change"],
            &cmas,
            VersionBumps::default()
        ),
        VersionBumps {
            major: true,
            ..Default::default()
        }
    );
    assert_eq!(
        calc_bumps(&["    feat: change"], &cmas, VersionBumps::default()),
        VersionBumps {
            minor: true,
            ..Default::default()
        }
    );
    assert_eq!(
        calc_bumps(&["    fix: change"], &cmas, VersionBumps::default()),
        VersionBumps {
            patch: true,
            ..Default::default()
        }
    );
    assert_eq!(
        calc_bumps(&["    patch: change"], &cmas, VersionBumps::default()),
        VersionBumps {
            patch: true,
            ..Default::default()
        }
    );
    assert_eq!(
        calc_bumps(
            &["    major(a component): change"],
            &cmas,
            VersionBumps::default()
        ),
        VersionBumps {
            major: true,
            ..Default::default()
        }
    );
    assert_eq!(
        calc_bumps(
            &["    feat(a component): change", "    BREAKING CHANGE"],
            &cmas,
            VersionBumps::default()
        ),
        VersionBumps {
            major: true,
            minor: true,
            ..Default::default()
        }
    );
    assert_eq!(
        calc_bumps(
            &["    feat(a component): change"],
            &cmas,
            VersionBumps::default()
        ),
        VersionBumps {
            minor: true,
            ..Default::default()
        }
    );
    assert_eq!(
        calc_bumps(
            &["    fix(a component): change"],
            &cmas,
            VersionBumps::default()
        ),
        VersionBumps {
            patch: true,
            ..Default::default()
        }
    );
    assert_eq!(
        calc_bumps(
            &["    patch(a component): change"],
            &cmas,
            VersionBumps::default()
        ),
        VersionBumps {
            patch: true,
            ..Default::default()
        }
    );
    assert_eq!(
        calc_bumps(&["    some random commit"], &cmas, VersionBumps::default()),
        VersionBumps::default()
    );
    assert_eq!(
        calc_bumps(
            &["    Merged PR: feat: change"],
            &cmas,
            VersionBumps::default()
        ),
        VersionBumps {
            minor: true,
            ..Default::default()
        }
    );
    assert_eq!(
        calc_bumps(
            &["    next-version: 9.4.3"],
            &cmas,
            VersionBumps::default()
        ),
        VersionBumps {
            commit_override: Some(Version::simple(9, 4, 3)),
            ..Default::default()
        }
    );
}

#[test]
fn mkver_calc_bumps_should_parse_real_log() {
    let full_log = "commit 6540bf8d6ac8ade4fc82ac8d73ba4e2739a1440a\nAuthor: Mona Lisa <mona.lisa@email.org>\nDate:   Tue May 19 18:25:04 2020 +1000\n\n    fix: code1.py";
    let lines: Vec<&str> = full_log.lines().collect();
    let cmas = default_commit_message_actions();
    assert_eq!(
        calc_bumps(&lines, &cmas, VersionBumps::default()),
        VersionBumps {
            patch: true,
            commit_count: 1,
            ..Default::default()
        }
    );
}

#[test]
fn mkver_get_last_version_should_compare_correctly() {
    let commit_infos = vec![
        CommitInfo {
            short_hash: "b3250df".to_string(),
            full_hash: "b3250df81f7ed389908a2aa89b32425a8ab8fb28".to_string(),
            commits_before_head: 0,
            tags: vec![
                Version::new(0, 1, 0, Some("RC1".to_string()), None),
                Version::simple(0, 1, 0),
            ],
        },
        CommitInfo {
            short_hash: "9ded7b1".to_string(),
            full_hash: "9ded7b1edf3c066b8c15839304d0427b06cdd020".to_string(),
            commits_before_head: 1,
            tags: vec![],
        },
    ];
    assert_eq!(
        get_last_version(&commit_infos),
        Some(LastVersion {
            commit_hash: "b3250df81f7ed389908a2aa89b32425a8ab8fb28".to_string(),
            commits_before_head: 0,
            version: Version::simple(0, 1, 0),
        })
    );
}

#[test]
fn mkver_get_commit_infos_should_parse_log_correctly() {
    let log = "\
10be55f 10be55fc56c197f5e0159cfbfac22832b289182f  (HEAD -> zio)
f971636 f9716367b8692ed582206951d72bc7affc150f41  (test)
298326d 298326dd43677121724e9589f390cb0279cb8708  (tag: v0.2.0, tag: v0.3.0)
2e79c27 2e79c27e2faf85ea241e1911788fd3582c5176ce
699068c 699068cdec9193878cc1fcfc44c7dd6d004621ff  (tag: other)
320ed50 320ed50d79cbd585d6a28842a340d9742d9327b1
b3250df b3250df81f7ed389908a2aa89b32425a8ab8fb28  (tag: v0.1.0-RC1, tag: v0.1.0)
9ded7b1 9ded7b1edf3c066b8c15839304d0427b06cdd020
";
    let git = MockGit {
        current_branch: "main".to_string(),
        full_log: String::new(),
        commit_info_log: log.to_string(),
    };
    let result = get_commit_infos(&git, "v").unwrap();
    assert_eq!(
        result,
        vec![
            CommitInfo {
                short_hash: "10be55f".to_string(),
                full_hash: "10be55fc56c197f5e0159cfbfac22832b289182f".to_string(),
                commits_before_head: 0,
                tags: vec![],
            },
            CommitInfo {
                short_hash: "f971636".to_string(),
                full_hash: "f9716367b8692ed582206951d72bc7affc150f41".to_string(),
                commits_before_head: 1,
                tags: vec![],
            },
            CommitInfo {
                short_hash: "298326d".to_string(),
                full_hash: "298326dd43677121724e9589f390cb0279cb8708".to_string(),
                commits_before_head: 2,
                tags: vec![Version::simple(0, 2, 0), Version::simple(0, 3, 0)],
            },
            CommitInfo {
                short_hash: "2e79c27".to_string(),
                full_hash: "2e79c27e2faf85ea241e1911788fd3582c5176ce".to_string(),
                commits_before_head: 3,
                tags: vec![],
            },
            CommitInfo {
                short_hash: "699068c".to_string(),
                full_hash: "699068cdec9193878cc1fcfc44c7dd6d004621ff".to_string(),
                commits_before_head: 4,
                tags: vec![],
            },
            CommitInfo {
                short_hash: "320ed50".to_string(),
                full_hash: "320ed50d79cbd585d6a28842a340d9742d9327b1".to_string(),
                commits_before_head: 5,
                tags: vec![],
            },
            CommitInfo {
                short_hash: "b3250df".to_string(),
                full_hash: "b3250df81f7ed389908a2aa89b32425a8ab8fb28".to_string(),
                commits_before_head: 6,
                tags: vec![
                    Version::new(0, 1, 0, Some("RC1".to_string()), None),
                    Version::simple(0, 1, 0)
                ],
            },
            CommitInfo {
                short_hash: "9ded7b1".to_string(),
                full_hash: "9ded7b1edf3c066b8c15839304d0427b06cdd020".to_string(),
                commits_before_head: 7,
                tags: vec![],
            },
        ]
    );
}

#[test]
fn mkver_format_version_should_format_version() {
    let version_data = VersionData {
        major: 1,
        minor: 2,
        patch: 3,
        pre_release_number: None,
        commit_count: 4,
        branch: "feature/f1".to_string(),
        commit_hash_short: "abcd".to_string(),
        commit_hash_full: "abcdefg".to_string(),
        date: Local::now().date_naive(),
    };
    let run_config = RunConfig {
        tag: true,
        tag_prefix: "v".to_string(),
        tag_message_format: "release {Version}".to_string(),
        pre_release_format: "RC".to_string(),
        build_meta_data_format: "{Branch}".to_string(),
        include_build_meta_data: false,
        commit_message_actions: default_commit_message_actions(),
        when_no_valid_commit_messages: IncrementAction::IncrementMinor,
        formats: vec![Format::new("Version", "{Major}.{Minor}.{Patch}")],
        patches: vec![],
    };
    assert_eq!(format_version(&run_config, &version_data, true, false), "v1.2.3");
}

#[test]
fn mkver_get_fallback_version_bumps() {
    assert_eq!(
        get_fallback_version_bumps(IncrementAction::Fail, VersionBumps::default()),
        Err("No valid commit messages found describing version increment".to_string())
    );
    assert_eq!(
        get_fallback_version_bumps(IncrementAction::IncrementMajor, VersionBumps::default()),
        Ok(VersionBumps {
            major: true,
            ..Default::default()
        })
    );
    assert_eq!(
        get_fallback_version_bumps(IncrementAction::IncrementMinor, VersionBumps::default()),
        Ok(VersionBumps {
            minor: true,
            ..Default::default()
        })
    );
    assert_eq!(
        get_fallback_version_bumps(IncrementAction::IncrementPatch, VersionBumps::default()),
        Ok(VersionBumps {
            patch: true,
            ..Default::default()
        })
    );
    assert_eq!(
        get_fallback_version_bumps(IncrementAction::NoIncrement, VersionBumps::default()),
        Ok(VersionBumps::default())
    );
}

// ============================================================================
// MainSpec
// ============================================================================

#[derive(Default)]
struct MockConsole {
    lines: Vec<String>,
}

impl Console for MockConsole {
    fn put_str_ln(&mut self, line: &str) {
        self.lines.push(line.to_string());
    }
}

#[test]
fn main_next_should_return() {
    let git = MockGit {
        current_branch: "main".to_string(),
        full_log: String::new(),
        commit_info_log: String::new(),
    };
    let mut console = MockConsole::default();
    main_impl(&["next".to_string()], &git, &mut console).unwrap();
    assert_eq!(console.lines, vec!["0.1.0"]);
}

// ============================================================================
// EndToEndTests
// ============================================================================

fn git_init(dir: &Path) {
    exec(&["git", "init", "--initial-branch=main"], Some(dir)).unwrap();
    exec(&["git", "config", "user.name", "Mona Lisa"], Some(dir)).unwrap();
    exec(
        &["git", "config", "user.email", "mona.lisa@email.org"],
        Some(dir),
    )
    .unwrap();
}

fn git_fix(name: &str, dir: &Path) {
    let _ = std::fs::File::create(dir.join(name));
    exec(&["git", "add", "."], Some(dir)).unwrap();
    exec(&["git", "commit", "-m", &format!("fix: {}", name)], Some(dir)).unwrap();
}

fn git_feat(name: &str, dir: &Path) {
    let _ = std::fs::File::create(dir.join(name));
    exec(&["git", "add", "."], Some(dir)).unwrap();
    exec(&["git", "commit", "-m", &format!("feat: {}", name)], Some(dir)).unwrap();
}

fn git_major(name: &str, dir: &Path) {
    let _ = std::fs::File::create(dir.join(name));
    exec(&["git", "add", "."], Some(dir)).unwrap();
    exec(&["git", "commit", "-m", &format!("major: {}", name)], Some(dir)).unwrap();
}

fn git_branch(name: &str, dir: &Path) {
    exec(&["git", "checkout", "-b", name], Some(dir)).unwrap();
}

fn git_merge(name: &str, dir: &Path) {
    exec(&["git", "merge", "--no-ff", name], Some(dir)).unwrap();
}

fn git_checkout(name: &str, dir: &Path) {
    exec(&["git", "checkout", name], Some(dir)).unwrap();
}

fn run_in_temp(dir: &Path, command: &str) -> Vec<String> {
    let git = LiveGit::new(Some(PathBuf::from(dir)));
    let mut console = MockConsole::default();
    main_impl(&[command.to_string()], &git, &mut console).unwrap();
    console.lines
}

#[test]
fn e2e_no_tags_should_return_version_0_1_0() {
    let temp = tempfile::tempdir().unwrap();
    let dir = temp.path();
    git_init(dir);
    git_fix("code1.py", dir);
    let out = run_in_temp(dir, "next");
    assert_eq!(out, vec!["0.1.0"]);
}

#[test]
fn e2e_main_advances_correctly_and_should_return_version_0_1_1() {
    let temp = tempfile::tempdir().unwrap();
    let dir = temp.path();
    git_init(dir);
    git_fix("code1.py", dir);
    run_in_temp(dir, "tag");
    git_fix("code2.py", dir);
    let out = run_in_temp(dir, "next");
    assert_eq!(out, vec!["0.1.1"]);
}

#[test]
fn e2e_feature_branch_minor_and_main_major_both_advance_version_and_should_return_1_0_0() {
    let temp = tempfile::tempdir().unwrap();
    let dir = temp.path();
    git_init(dir);
    git_fix("code1.py", dir);
    run_in_temp(dir, "tag");
    git_branch("feature/f1", dir);
    git_feat("code2.py", dir);
    git_checkout("main", dir);
    git_major("code3.py", dir);
    git_merge("feature/f1", dir);
    let out = run_in_temp(dir, "next");
    assert_eq!(out, vec!["1.0.0"]);
}

#[test]
fn e2e_feature_branch_major_and_main_minor_both_advance_version_and_should_return_1_0_0() {
    let temp = tempfile::tempdir().unwrap();
    let dir = temp.path();
    git_init(dir);
    git_fix("code1.py", dir);
    run_in_temp(dir, "tag");
    git_branch("feature/f1", dir);
    git_major("code2.py", dir);
    git_checkout("main", dir);
    git_feat("code3.py", dir);
    git_merge("feature/f1", dir);
    let out = run_in_temp(dir, "next");
    assert_eq!(out, vec!["1.0.0"]);
}

#[test]
fn e2e_feature_branch_1_major_and_feature_branch_2_minor_both_advance_version_and_should_return_1_0_0() {
    let temp = tempfile::tempdir().unwrap();
    let dir = temp.path();
    git_init(dir);
    git_fix("code1.py", dir);
    run_in_temp(dir, "tag");

    git_branch("feature/f1", dir);
    git_feat("code2.py", dir);
    git_checkout("main", dir);

    git_branch("feature/f2", dir);
    git_major("code3.py", dir);
    git_checkout("main", dir);

    git_merge("feature/f1", dir);
    git_merge("feature/f2", dir);

    let out = run_in_temp(dir, "next");
    assert_eq!(out, vec!["1.0.0"]);
}
