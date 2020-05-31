class GitMkver < Formula
  MKVER_VERSION = "1.0.0".freeze
  MKVER_SHA256  = "d1546df319e236d038f7c6b3b88ca155fb882b237011c2cbd7e56e9a16b6ba56".freeze

  desc "Installs git-mkver from pre-built binaries"
  homepage "https://idc101.github.io/git-mkver/"
  url "https://github.com/idc101/git-mkver/releases/download/v#{MKVER_VERSION}/git-mkver-darwin-amd64-#{MKVER_VERSION}.tar.gz"
  version MKVER_VERSION
  sha256 MKVER_SHA256

  def install
    bin.install 'git-mkver'
  end
end
