#!/bin/bash
set -e

sbt -error -batch "run -c git-mkver.conf patch"
version=`sbt -error -batch "run -c git-mkver.conf next"`
arch=`arch`

sbt assembly

# Mac
if [[ "$(uname)" == "Darwin" ]]
then
  pushd target
  native-image -H:IncludeResources='.*conf$' --no-fallback -jar scala-2.12/git-mkver-assembly-$version.jar
  mv git-mkver-assembly-$version git-mkver-darwin-$arch-$version
  cp git-mkver-darwin-$arch-$version git-mkver
  chmod +x git-mkver
  tar -cvzf git-mkver-darwin-$arch-$version.tar.gz git-mkver
  rm git-mkver
  popd

  DARWIN_SHA256=$(openssl dgst -sha256 target/git-mkver-darwin-$arch-$version.tar.gz | cut -f2 -d' ')
  echo "DARWIN_SHA256=$DARWIN_SHA256"
  #sed -i '' -e "s/MKVER_SHA256  = \".*\".freeze/MKVER_SHA256  = \"$DARWIN_SHA256\".freeze/g" /usr/local/Homebrew/Library/Taps/idc101/homebrew-gitmkver/Casks/git-mkver.rb
fi

# Linux
if [[ "$(uname)" == "Linux" ]]
then
  pushd target
  native-image -H:IncludeResources='.*conf$' --no-fallback -jar scala-2.12/git-mkver-assembly-$version.jar
  mv git-mkver-assembly-$version git-mkver-linux-$arch-$version
  cp git-mkver-linux-$arch-$version git-mkver
  chmod +x git-mkver
  tar -cvzf git-mkver-linux-$arch-$version.tar.gz git-mkver
  rm git-mkver
  popd

  LINUX_SHA256=$(openssl dgst -sha256 target/git-mkver-linux-$arch-$version.tar.gz | cut -f2 -d' ')
  echo "LINUX_SHA256=$LINUX_SHA256"
fi

# build universal
sbt universal:packageBin
UNIVERSAL_SHA256=$(openssl dgst -sha256 target/universal/git-mkver-$version.zip | cut -f2 -d' ')
echo "UNIVERSAL_SHA256=$UNIVERSAL_SHA256"