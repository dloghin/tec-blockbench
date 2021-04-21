#!/bin/bash

#PEER_COUNT=$1
#ORDERER_COUNT=$2
PEER_COUNT=4
ORDERER_COUNT=3

SRC_DIR="git/blockbench/benchmark/fabric2"
NODEJS_DIR="/home/ubuntu/tools/node-v14.16.1-linux-arm64/bin"
CHANNEL="testchannel"
NETWORK_DIR="test"
BLOCK_SIZE=500

CC_NAME=kvstore
ALL_ORG=""

MODE=open_loop
ORG_ID=1

PEERS=""
for IDX in `seq 1 $PEER_COUNT`; do
        PEERS="$PEERS jetsontxx0$IDX.master"
done

ORDERERS=""
START_IDX=$(($PEER_COUNT+1))
END_IDX=$(($PEER_COUNT+$ORDERER_COUNT))
for IDX in `seq $START_IDX $END_IDX`; do
        ORDERERS="$ORDERERS jetsontxx0$IDX.master"
done

