use std::env;
use std::process::ExitCode;
use git_mkver::git::LiveGit;
use git_mkver::{main_impl, StdConsole};

fn main() -> ExitCode {
    let args: Vec<String> = env::args().skip(1).collect();
    let git = LiveGit::new(None);
    let mut console = StdConsole;
    match main_impl(&args, &git, &mut console) {
        Ok(()) => ExitCode::SUCCESS,
        Err(err) => {
            println!("{}", err);
            ExitCode::FAILURE
        }
    }
}
