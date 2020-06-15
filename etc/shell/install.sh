#/bin/bash
MKVER_VERSION=1.1.1
curl -L https://github.com/idc101/git-mkver/releases/download/v${MKVER_VERSION}/git-mkver-darwin-amd64-${MKVER_VERSION}.tar.gz -o git-mkver.tar.gz
tar xvzf git-mkver.tar.gz
sudo mv git-mkver /usr/local/bin
rm git-mkver.tar.gz
