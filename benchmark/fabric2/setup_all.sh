#!/bin/bash

set -x

. ./env.sh

# clean and regenerate test network
function prepare {

for PEER in $PEERS; do
        ssh $PEER "cd $SRC_DIR && rm -rf $NETWORK_DIR"
done
for ORDERER in $ORDERERS; do
	ssh $ORDERER "cd $SRC_DIR && rm -rf $NETWORK_DIR"
done
rm -rf $NETWORK_DIR
python prepare.py $NETWORK_DIR $CHANNEL $BLOCK_SIZE $ORDERER_COUNT $ORDERERS $PEER_COUNT $PEERS

for PEER in $PEERS; do
	scp -r $NETWORK_DIR $PEER:$SRC_DIR/
done
for ORDERER in $ORDERERS; do
	scp -r $NETWORK_DIR $ORDERER:$SRC_DIR/
done

}

function deploy {

./setup_channel.sh $NETWORK_DIR $CHANNEL

for IDX in `seq ${PEER_COUNT}`; do
   ALL_ORG="$ALL_ORG 'Org${IDX}MSP.peer'"
done

function join_by { local d=$1; shift; local f=$1; shift; printf %s "$f" "${@/#/$d}"; }
ENDORSE_POLICY="AND($(join_by , $ALL_ORG))"

./deployCC.sh $NETWORK_DIR $CHANNEL $CC_NAME "$ENDORSE_POLICY" "InitLedger" "InitLedger"

}

# prepare

python network.py $NETWORK_DIR on

./setup_channel.sh $NETWORK_DIR $CHANNEL

deploy

for PEER in $PEERS; do
	ssh $PEER "PATH=$PATH:$NODEJS_DIR && cd $SRC_DIR && nohup node blockbench/txn-server.js $NETWORK_DIR $ORG_ID $CHANNEL $CC_NAME $MODE 8801 > txn-server-$PEER.log 2>&1 &"
	ssh $PEER "PATH=$PATH:$NODEJS_DIR && cd $SRC_DIR && nohup node blockbench/block-server.js $NETWORK_DIR $ORG_ID $CHANNEL 8800 > block-server-$PEER.log 2>&1 &"
done
