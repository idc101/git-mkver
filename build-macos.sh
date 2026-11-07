#!/bin/bash
set -e

cargo run --quiet -- -c git-mkver.conf patch
version=$(cargo run --quiet -- -c git-mkver.conf next)
arch=$(arch)

cargo build --release

pushd target
cp release/git-mkver git-mkver-darwin-$arch-$version
cp git-mkver-darwin-$arch-$version git-mkver
chmod +x git-mkver
tar -cvzf git-mkver-darwin-$arch-$version.tar.gz git-mkver
rm git-mkver
popd

DARWIN_SHA256=$(openssl dgst -sha256 target/git-mkver-darwin-$arch-$version.tar.gz | cut -f2 -d' ')
echo "DARWIN_SHA256=$DARWIN_SHA256"
