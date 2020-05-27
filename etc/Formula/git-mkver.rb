class GitMkver < Formula
  MKVER_VERSION = "0.6.0+feature-pre-release.76ac5bd-RC+feature-pre-release.76ac5bd".freeze
  MKVER_SHA256  = "46c5506fa32d0f895078c9ba0bb965aab86033fa34bee7f941603cb74f607ad6".freeze

  desc "Installs git-mkver from pre-built binaries"
  homepage "https://idc101.github.io/git-mkver/"
  url "https://github.com/idc101/git-mkver/releases/download/v#{MKVER_VERSION}/git-mkver-darwin-amd64-#{MKVER_VERSION}.tar.gz"
  version MKVER_VERSION
  sha256 MKVER_SHA256

  def install
    bin.install 'git-mkver'
  end
end
