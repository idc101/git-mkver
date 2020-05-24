class GitMkver < Formula
  MKVER_VERSION = "0.5.1".freeze
  MKVER_SHA256  = "60b12160b0754e5e9d0b631ae0b8537d6aca33a77e6aff240b049c0dac17dbaf".freeze

  desc "Installs git-mkver from pre-built binaries"
  homepage "https://idc101.github.io/git-mkver/"
  url "https://github.com/idc101/git-mkver/releases/download/v#{MKVER_VERSION}/git-mkver-darwin-amd64-#{MKVER_VERSION}.tar.gz"
  version MKVER_VERSION
  sha256 MKVER_SHA256

  def install
    bin.install 'git-mkver'
  end
end
