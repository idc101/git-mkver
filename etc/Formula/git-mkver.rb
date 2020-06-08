class GitMkver < Formula
  MKVER_VERSION = "1.1.0".freeze
  MKVER_SHA256  = "f69fb9b97f510b05455138fc202b53aa5d3f55af471d995bbe74888aeaea28db".freeze

  desc "Installs git-mkver from pre-built binaries"
  homepage "https://idc101.github.io/git-mkver/"
  url "https://github.com/idc101/git-mkver/releases/download/v#{MKVER_VERSION}/git-mkver-darwin-amd64-#{MKVER_VERSION}.tar.gz"
  version MKVER_VERSION
  sha256 MKVER_SHA256

  def install
    bin.install 'git-mkver'
  end
end
