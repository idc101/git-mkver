#!/bin/bash
set -e

cargo run --quiet -- -c git-mkver.conf patch
version=$(cargo run --quiet -- -c git-mkver.conf next)

rustup target add x86_64-apple-darwin aarch64-apple-darwin
cargo build --release --target x86_64-apple-darwin
cargo build --release --target aarch64-apple-darwin

pushd target
lipo -create -output git-mkver-darwin-universal-$version \
  x86_64-apple-darwin/release/git-mkver \
  aarch64-apple-darwin/release/git-mkver
cp git-mkver-darwin-universal-$version git-mkver
chmod +x git-mkver
tar -cvzf git-mkver-darwin-universal-$version.tar.gz git-mkver
rm git-mkver
popd

DARWIN_SHA256=$(openssl dgst -sha256 target/git-mkver-darwin-universal-$version.tar.gz | cut -f2 -d' ')
echo "DARWIN_SHA256=$DARWIN_SHA256"
