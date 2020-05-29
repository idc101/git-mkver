class GitMkver < Formula
  MKVER_VERSION = "1.0.0".freeze
  MKVER_SHA256  = "0c2dd179c28272e585370a01cf2fd33277bb6d1ce15952569b31a9a5c3776c57".freeze

  desc "Installs git-mkver from pre-built binaries"
  homepage "https://idc101.github.io/git-mkver/"
  url "https://github.com/idc101/git-mkver/releases/download/v#{MKVER_VERSION}/git-mkver-darwin-amd64-#{MKVER_VERSION}.tar.gz"
  version MKVER_VERSION
  sha256 MKVER_SHA256

  def install
    bin.install 'git-mkver'
  end
end
