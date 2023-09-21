#/bin/bash
MKVER_VERSION=1.4.0

# Mac
if [[ "$(uname)" == "Darwin" ]]
then
  curl -L https://github.com/idc101/git-mkver/releases/download/v${MKVER_VERSION}/git-mkver-darwin-i386-${MKVER_VERSION}.tar.gz -o git-mkver.tar.gz
  tar xvzf git-mkver.tar.gz
  sudo mv git-mkver /usr/local/bin
  rm git-mkver.tar.gz
# Linux
elif [[ "$(uname)" == "Linux" ]]
then
  curl -L https://github.com/idc101/git-mkver/releases/download/v${MKVER_VERSION}/git-mkver-linux-x86_64-${MKVER_VERSION}.tar.gz -o git-mkver.tar.gz
  tar xvzf git-mkver.tar.gz
  sudo mv git-mkver /usr/bin
  rm git-mkver.tar.gz
fi