class GitMkver < Formula
  MKVER_VERSION = "1.1.1".freeze
  MKVER_SHA256  = "9f6c6d5b4f96c9f3f3da9d58aee8d9f133c706db3cfb915e48e3a3d1964cede2".freeze

  desc "Installs git-mkver from pre-built binaries"
  homepage "https://idc101.github.io/git-mkver/"
  url "https://github.com/idc101/git-mkver/releases/download/v#{MKVER_VERSION}/git-mkver-darwin-amd64-#{MKVER_VERSION}.tar.gz"
  version MKVER_VERSION
  sha256 MKVER_SHA256

  def install
    bin.install 'git-mkver'
  end
end
