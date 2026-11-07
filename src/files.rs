use std::fs;
use std::path::{Path, PathBuf};
use regex::Regex;
use walkdir::WalkDir;

pub fn exists(path: &Path) -> bool {
    path.exists()
}

/// Convert a Java `FileSystem.getPathMatcher("glob:...")` pattern into a Regex.
/// In Java NIO glob syntax:
/// - `**` crosses directory boundaries
/// - `*` matches any characters except directory separator
/// - `?` matches a single character except directory separator
pub fn glob_to_regex(glob: &str) -> Result<Regex, String> {
    let sep = std::path::MAIN_SEPARATOR;
    let sep_escaped = regex::escape(&sep.to_string());
    let mut regex_str = String::from("^");
    let chars: Vec<char> = glob.chars().collect();
    let mut i = 0;

    while i < chars.len() {
        let c = chars[i];
        match c {
            '*' => {
                if i + 1 < chars.len() && chars[i + 1] == '*' {
                    // Count how many consecutive '*' we have (e.g. `***` in `***.sbt`)
                    while i < chars.len() && chars[i] == '*' {
                        i += 1;
                    }
                    regex_str.push_str(".*");
                    continue;
                } else {
                    regex_str.push_str(&format!("[^{}]*", sep_escaped));
                }
            }
            '?' => {
                regex_str.push_str(&format!("[^{}]", sep_escaped));
            }
            '/' | '\\' => {
                regex_str.push_str(&sep_escaped);
            }
            '.' | '+' | '(' | ')' | '|' | '^' | '$' | '@' | '%' => {
                regex_str.push('\\');
                regex_str.push(c);
            }
            '[' => {
                regex_str.push('[');
            }
            ']' => {
                regex_str.push(']');
            }
            '{' => {
                regex_str.push_str("(?:");
            }
            '}' => {
                regex_str.push(')');
            }
            ',' => {
                regex_str.push('|');
            }
            _ => {
                regex_str.push(c);
            }
        }
        i += 1;
    }

    regex_str.push('$');
    Regex::new(&regex_str).map_err(|e| e.to_string())
}

pub fn glob(base: &Path, pattern: &str) -> Result<Vec<PathBuf>, String> {
    let matcher = glob_to_regex(pattern)?;
    let walk_root = if base.as_os_str().is_empty() {
        Path::new(".")
    } else {
        base
    };

    let mut matches = Vec::new();
    for entry in WalkDir::new(walk_root) {
        let entry = entry.map_err(|e| e.to_string())?;
        let path = entry.path();

        let candidate = if base.as_os_str().is_empty() {
            path.strip_prefix(".").unwrap_or(path).to_path_buf()
        } else {
            path.to_path_buf()
        };

        let candidate_str = candidate.to_string_lossy();
        if matcher.is_match(&candidate_str) {
            matches.push(candidate);
        }
    }
    Ok(matches)
}

pub fn read_all(path: &Path) -> Result<String, String> {
    fs::read_to_string(path).map_err(|e| e.to_string())
}

pub fn write(path: &Path, content: &str) -> Result<(), String> {
    fs::write(path, content.as_bytes()).map_err(|e| e.to_string())
}
