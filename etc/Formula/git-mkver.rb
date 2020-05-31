class GitMkver < Formula
  MKVER_VERSION = "1.0.0".freeze
  MKVER_SHA256  = "2d935103e4b942a5153de9a04a4fe70455d39284bc22378db1e0cc2bb024f61f".freeze

  desc "Installs git-mkver from pre-built binaries"
  homepage "https://idc101.github.io/git-mkver/"
  url "https://github.com/idc101/git-mkver/releases/download/v#{MKVER_VERSION}/git-mkver-darwin-amd64-#{MKVER_VERSION}.tar.gz"
  version MKVER_VERSION
  sha256 MKVER_SHA256

  def install
    bin.install 'git-mkver'
  end
end
