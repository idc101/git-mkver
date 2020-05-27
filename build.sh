#!/bin/bash
set -e

# Note requires a previous version of git-mkver to have been built and on the path!
git mkver -c git-mkver.conf patch
version=`git mkver -c git-mkver.conf next`

sbt assembly

# Mac
if [[ "$(uname)" == "Darwin" ]]
then
  pushd target
  native-image -H:IncludeResources='.*conf$' --no-fallback -jar scala-2.12/git-mkver-assembly-$version.jar
  mv git-mkver-assembly-$version git-mkver-darwin-amd64-$version
  cp git-mkver-darwin-amd64-$version git-mkver
  chmod +x git-mkver
  tar -cvzf git-mkver-darwin-amd64-$version.tar.gz git-mkver
  rm git-mkver
  popd

  # Linux
  docker run -v $(pwd):/workspace -it git-mkver \
    /bin/bash -c "cd /workspace/target; native-image -H:IncludeResources='.*conf$' --no-fallback -jar scala-2.12/git-mkver-assembly-$version.jar; mv git-mkver-assembly-$version git-mkver-linux-amd64-$version"

  pushd target
  cp git-mkver-linux-amd64-$version git-mkver
  chmod +x git-mkver
  tar -cvzf git-mkver-linux-amd64-$version.tar.gz git-mkver
  rm git-mkver
  popd

  DARWIN_SHA256=$(openssl dgst -sha256 target/git-mkver-darwin-amd64-$version.tar.gz | cut -f2 -d' ')
  LINUX_SHA256=$(openssl dgst -sha256 target/git-mkver-linux-amd64-$version.tar.gz | cut -f2 -d' ')

  sed -i '' -e "s/MKVER_SHA256  = \".*\".freeze/MKVER_SHA256  = \"$DARWIN_SHA256\".freeze/g" etc/Formula/git-mkver.rb
fi

# Linux
if [[ "$(uname)" == "Linux" ]]
then
  pushd target
  native-image -H:IncludeResources='.*conf$' --no-fallback -jar scala-2.12/git-mkver-assembly-$version.jar
  mv git-mkver-assembly-$version git-mkver-linux-amd64-$version
  cp git-mkver-linux-amd64-$version git-mkver
  chmod +x git-mkver
  tar -cvzf git-mkver-linux-amd64-$version.tar.gz git-mkver
  rm git-mkver
  popd
fi
