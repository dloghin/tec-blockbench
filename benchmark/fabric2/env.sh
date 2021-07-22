#!/bin/bash

# Edit the following to match your setup
#PEER_COUNT=$1
#ORDERER_COUNT=$2

PEER_COUNT=10
ORDERER_COUNT=1

# On CIIDAA
#SRC_DIR="/users/dumi/git/blockbench/benchmark/fabric2"
#NODEJS_DIR="/users/dumi/tools/node-v14.16.1-linux-x64/bin"
#DRIVER="/users/dumi/git/blockbench/src/macro/kvstore/driver"
#WORKLOAD="/users/dumi/git/blockbench/src/macro/kvstore/workloads/workloada.spec"
#FABRIC_DATA_DIR="/data/dumi/fabric_data"

# On Jetson & AWS
SRC_DIR="/home/ubuntu/git/blockbench/benchmark/fabric2"
# ! For x64
NODEJS_DIR="/home/ubuntu/tools/node-v14.16.1-linux-x64/bin"
# ! For ARM64 (aarch64)
#NODEJS_DIR="/home/ubuntu/tools/node-v14.16.1-linux-arm64/bin"
DRIVER="/home/ubuntu/git/blockbench/src/macro/kvstore/driver"
WORKLOAD="/home/ubuntu/git/blockbench/src/macro/kvstore/workloads/workloada.spec"
FABRIC_DATA_DIR="/home/ubuntu/fabric_data"

# On Docker
# SRC_DIR="/git/blockbench/benchmark/fabric2"
#NODEJS_DIR="/tools/node-v14.16.1-linux-x64/bin"
#DRIVER="/git/blockbench/src/macro/kvstore/driver"
#WORKLOAD="/git/blockbench/src/macro/kvstore/workloads/workloada.spec"
#USER="root"
#FABRIC_DATA_DIR="/fabric_data"

# Driver runtime duration (seconds)
DURATION=120

TXRATES="5 10 15 20 25 30 35 40 45 50 55 60 65 70 75 80 85 90 95 100"

DRIVERS_PER_CLIENT=16

# Comment if you do not have a power meter
# POWER_METER_APP="wucomp"
# POWER_METER_CMD="wucomp /dev/ttyUSB0"

# If you have a shared (NFS) folder, please uncomment
# SHARED=1

# Do not edit the following
CHANNEL="testchannel"
NETWORK_DIR="test"
BLOCK_SIZE=500
CC_NAME=kvstore
ALL_ORG=""
MODE=open_loop
ORG_ID=1

# You may edit the following to match your setup.
# On AWS, just put all the private IPs of the instances in PEERS.
PEERS=""
START_IDX=2
END_IDX=$(($START_IDX+$PEER_COUNT-1))
for IDX in `seq $START_IDX $END_IDX`; do
	# On Jetson
	# PEER=`printf jetsontxx%02d.master $IDX`
	# On CIIDAA
	# PEER="slave-$IDX"
	# On Docker
	PEER="192.168.1.$IDX"
	PEERS="$PEERS $PEER"
done

IDX=$(($END_IDX+1))
ORDERERS="192.168.1.$IDX"
#START_IDX=1
#END_IDX=$PEER_COUNT
#for IDX in `seq $START_IDX $END_IDX`; do
#        # On Jetson
#        PEER=`printf jetsontxx%02d.master $IDX`
#        # On CIIDAA
#        # PEER="slave-$IDX"
#	ORDERERS="$ORDERERS $PEER"
#done
