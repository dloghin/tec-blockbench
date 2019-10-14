#!/bin/bash
#args: number_of_nodes
cd `dirname ${BASH_SOURCE-$0}`
. env.sh

let i=$1+1
echo "[*] init.sh $i, $1, $2"

mkdir -p $QUO_DATA/dd$i/{keystore,geth}

cp pbft/nodekey$2/static-nodes.json $QUO_DATA/dd$i/static-nodes.json
cp pbft/nodekey$2/$1/nodekey $QUO_DATA/dd$i/geth/nodekey
cp keys/key$i $QUO_DATA/dd$i/keystore
${QUORUM} --datadir=$QUO_DATA/dd$i init $QUO_HOME/pbft/nodekey$2/genesis.json
${QUORUM} --datadir=$QUO_DATA/dd$i account new --password <(echo -n "")
