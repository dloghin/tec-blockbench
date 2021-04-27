#!/bin/bash

# Edit the following to match your setup
#PEER_COUNT=$1
#ORDERER_COUNT=$2
PEER_COUNT=10
ORDERER_COUNT=1
SRC_DIR="git/blockbench/benchmark/fabric2"
NODEJS_DIR="/home/ubuntu/tools/node-v14.16.1-linux-arm64/bin"

# Do not edit the following
CHANNEL="testchannel"
NETWORK_DIR="test"
BLOCK_SIZE=500
CC_NAME=kvstore
ALL_ORG=""
MODE=open_loop
ORG_ID=1

# You may edit the following to match your setup
PEERS=""
START_IDX=1
END_IDX=$PEER_COUNT
for IDX in `seq $START_IDX $END_IDX`; do
	# On Jetson
	PEER=`printf jetsontxx%02d.master $IDX`
	# On CIIDAA
	# PEER="slave-$IDX"
	PEERS="$PEERS $PEER"
done

ORDERERS=""
START_IDX=1
END_IDX=$PEER_COUNT
for IDX in `seq $START_IDX $END_IDX`; do
        # On Jetson
        PEER=`printf jetsontxx%02d.master $IDX`
        # On CIIDAA
        # PEER="slave-$IDX"
	ORDERERS="$ORDERERS $PEER"
done
