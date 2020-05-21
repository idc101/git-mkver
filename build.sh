#!/bin/bash
set -e

# Note requires a previous version of git-mkver to have been built and on the path!
git mkver patch
version=`git mkver next`

sbt assembly

# Mac
if [[ "$(uname)" == "Darwin "]]
then
  pushd target
  native-image -H:IncludeResources='.*conf$' --no-fallback -jar scala-2.12/git-mkver-assembly-$version.jar
  mv git-mkver-assembly-$version git-mkver-darwin-amd64-$version
  popd

  # Linux
  docker run -v $(pwd):/workspace -it git-mkver \
    /bin/bash -c "cd /workspace/target; native-image -H:IncludeResources='.*conf$' --no-fallback -jar scala-2.12/git-mkver-assembly-$version.jar; mv git-mkver-assembly-$version git-mkver-linux-amd64-$version"
fi

# Linux
if [[ "$(uname)" == "Linux "]]
then
  pushd target
  native-image -H:IncludeResources='.*conf$' --no-fallback -jar scala-2.12/git-mkver-assembly-$version.jar
  mv git-mkver-assembly-$version git-mkver-linux-amd64-$version
  popd
fi
