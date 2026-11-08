#!/bin/bash
set -e

cargo run --quiet -- -c git-mkver.conf patch
version=$(cargo run --quiet -- -c git-mkver.conf next -f '{Version}')
arch=$(arch)

cargo build --release

pushd target
cp release/git-mkver git-mkver-linux-$arch-$version
cp git-mkver-linux-$arch-$version git-mkver
chmod +x git-mkver
tar -cvzf git-mkver-linux-$arch-$version.tar.gz git-mkver
rm git-mkver
popd

LINUX_SHA256=$(openssl dgst -sha256 target/git-mkver-linux-$arch-$version.tar.gz | cut -f2 -d' ')
echo "LINUX_SHA256=$LINUX_SHA256"
