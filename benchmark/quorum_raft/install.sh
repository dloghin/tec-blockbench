#!/bin/bash

. env.sh

# Install go
export GOROOT=$QUO_HOME/go
export PATH=$GOROOT/bin:$PATH
export GOPATH=$GO_PATH
export PATH=$GOPATH/bin:$PATH
if ! [ `command -v go` ]; then
  mkdir -p go
  wget https://dl.google.com/go/go1.9.3.linux-amd64.tar.gz
  tar -xzvf go1.9.3.linux-amd64.tar.gz
  rm go1.9.3.linux-amd64.tar.gz
fi

# installing quorum
if ! [ -d quorum ]; then
  git clone https://github.com/jpmorganchase/quorum.git
fi
cd quorum
make all

#Binaries are placed in $REPO_ROOT/build/bin. Put that folder in your PATH to make geth and bootnode easily invokable, or copy those binaries to a folder already in PATH, e.g. /usr/local/bin.
#An easy way to supplement PATH is to add PATH=$PATH:/path/to/repository/build/bin to your ~/.bashrc or ~/.bash_aliases file.
#[quorum-Getting-Set-Up]https://github.com/jpmorganchase/quorum/wiki/Getting-Set-Up
