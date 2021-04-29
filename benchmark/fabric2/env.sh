#!/bin/bash

# Edit the following to match your setup
#PEER_COUNT=$1
#ORDERER_COUNT=$2

PEER_COUNT=8
ORDERER_COUNT=1
SRC_DIR="/users/dumi/git/blockbench/benchmark/fabric2"
NODEJS_DIR="/users/dumi/tools/node-v14.16.1-linux-x64/bin"

DRIVER="/users/dumi/git/blockbench/src/macro/kvstore/driver"
WORKLOAD="/users/dumi/git/blockbench/src/macro/kvstore/workloads/workloada.spec"

# Driver runtime duration (seconds)
DURATION=120

# TXRATES="5 10 15 20 25 30 35 40 45 50 55 60 65 70 75 80 85 90 95 100"
TXRATES="5 10 15 20 25 30 35 40 45 50 55 60"
# TXRATES="50 75 100 125 150 175 200 225 250"
# TXRATES="5 20 40 60 80 100 200 300 400 500"
# TXRATES="200"

DRIVERS_PER_CLIENT=16

# Comment if you do not have a power meter
POWER_METER_APP="wucomp"
POWER_METER_CMD="wucomp /dev/ttyUSB0"

# If you have a shared (NFS) folder, please uncomment
SHARED=1

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
START_IDX=81
END_IDX=88
for IDX in `seq $START_IDX $END_IDX`; do
	# On Jetson
	# PEER=`printf jetsontxx%02d.master $IDX`
	# On CIIDAA
	PEER="slave-$IDX"
	PEERS="$PEERS $PEER"
done

ORDERERS="slave-92"
#START_IDX=1
#END_IDX=$PEER_COUNT
#for IDX in `seq $START_IDX $END_IDX`; do
#        # On Jetson
#        PEER=`printf jetsontxx%02d.master $IDX`
#        # On CIIDAA
#        # PEER="slave-$IDX"
#	ORDERERS="$ORDERERS $PEER"
#done
