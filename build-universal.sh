#!/bin/bash
set -e

sbt -error -batch "run -c git-mkver.conf patch"
version=`sbt -error -batch "run -c git-mkver.conf next"`
arch=`arch`

sbt assembly

# build universal
sbt universal:packageBin
UNIVERSAL_SHA256=$(openssl dgst -sha256 target/universal/git-mkver-$version.zip | cut -f2 -d' ')
echo "UNIVERSAL_SHA256=$UNIVERSAL_SHA256"