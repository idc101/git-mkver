use std::env;
use std::path::PathBuf;
use std::process::Command;

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct ProcessResult {
    pub stdout: String,
    pub stderr: String,
    pub exit_code: i32,
}

pub trait Git {
    fn current_branch(&self) -> Result<String, String>;
    fn full_log(&self, from_ref: Option<&str>) -> Result<String, String>;
    fn commit_info_log(&self) -> Result<String, String>;
    fn tag(&self, tag: &str, tag_message: &str) -> Result<(), String>;
    fn check_git_repo(&self) -> Result<(), String>;
}

#[derive(Debug, Clone, Default)]
pub struct LiveGit {
    pub working_dir: Option<PathBuf>,
}

impl LiveGit {
    pub fn new(working_dir: Option<PathBuf>) -> Self {
        Self { working_dir }
    }
}

impl Git for LiveGit {
    fn current_branch(&self) -> Result<String, String> {
        if let Ok(branch) = env::var("BUILD_SOURCEBRANCH") {
            // Azure DevOps Pipeline
            Ok(branch
                .replace("refs/heads/", "")
                .replace("refs/", ""))
        } else if let Ok(branch) = env::var("CI_COMMIT_REF_NAME") {
            // GitLab CI
            Ok(branch)
        } else {
            exec(&["git", "rev-parse", "--abbrev-ref", "HEAD"], self.working_dir.as_deref())
                .map(|r| r.stdout)
        }
    }

    fn commit_info_log(&self) -> Result<String, String> {
        exec(
            &["git", "log", "--pretty=%h %H %d"],
            self.working_dir.as_deref(),
        )
        .map(|r| r.stdout)
    }

    fn full_log(&self, from_ref: Option<&str>) -> Result<String, String> {
        let mut args = vec!["git", "--no-pager", "log"];
        let range;
        if let Some(r) = from_ref {
            range = format!("{}..HEAD", r);
            args.push(&range);
        }
        exec(&args, self.working_dir.as_deref()).map(|r| r.stdout)
    }

    fn tag(&self, tag: &str, tag_message: &str) -> Result<(), String> {
        exec(
            &["git", "tag", "-a", "-m", tag_message, tag],
            self.working_dir.as_deref(),
        )
        .map(|_| ())
    }

    fn check_git_repo(&self) -> Result<(), String> {
        exec(&["git", "--no-pager", "show"], self.working_dir.as_deref()).and_then(|output| {
            if output.exit_code != 0 {
                Err(output.stdout)
            } else {
                Ok(())
            }
        })
    }
}

pub fn exec(commands: &[&str], dir: Option<&std::path::Path>) -> Result<ProcessResult, String> {
    if commands.is_empty() {
        return Err("Empty command".to_string());
    }
    let process_name = commands[0];
    let args = &commands[1..];
    let mut cmd = Command::new(process_name);
    cmd.args(args);
    if let Some(d) = dir {
        cmd.current_dir(d);
    }

    let output = cmd.output().map_err(|e| e.to_string())?;
    let stdout = String::from_utf8_lossy(&output.stdout).trim().to_string();
    let stderr = String::from_utf8_lossy(&output.stderr).trim().to_string();
    let exit_code = output.status.code().unwrap_or(1);

    if exit_code != 0 {
        Err(format!("Git error: {}", stderr))
    } else {
        Ok(ProcessResult {
            stdout,
            stderr,
            exit_code,
        })
    }
}
