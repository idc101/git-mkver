class GitMkver < Formula
  MKVER_VERSION = "1.2.0".freeze
  MKVER_SHA256  = "c4c0c0f6111d71d82d4595b2194d46f4c07a3db0bb639096aef7746a257a04bc".freeze

  desc "Installs git-mkver from pre-built binaries"
  homepage "https://idc101.github.io/git-mkver/"
  url "https://github.com/idc101/git-mkver/releases/download/v#{MKVER_VERSION}/git-mkver-darwin-amd64-#{MKVER_VERSION}.tar.gz"
  version MKVER_VERSION
  sha256 MKVER_SHA256

  def install
    bin.install 'git-mkver'
  end
end
