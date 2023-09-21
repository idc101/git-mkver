#!/bin/bash
set -e

sbt -error -batch "run -c git-mkver.conf patch"
version=`sbt -error -batch "run -c git-mkver.conf next"`
arch=`arch`

sbt assembly

pushd target
native-image --static -H:IncludeResources='.*conf$' --no-fallback -jar scala-2.12/git-mkver-assembly-$version.jar
mv git-mkver-assembly-$version git-mkver-linux-$arch-$version
cp git-mkver-linux-$arch-$version git-mkver
chmod +x git-mkver
tar -cvzf git-mkver-linux-$arch-$version.tar.gz git-mkver
rm git-mkver
popd

LINUX_SHA256=$(openssl dgst -sha256 target/git-mkver-linux-$arch-$version.tar.gz | cut -f2 -d' ')
echo "LINUX_SHA256=$LINUX_SHA256"
