#!/bin/bash

set -x

. ./env.sh

# clean and regenerate test network
function prepare {

for PEER in $PEERS; do
    ssh $PEER "cd $SRC_DIR && rm -rf $NETWORK_DIR"
	# ssh $PEER "sudo timedatectl set-ntp off; sleep 1; sudo timedatectl set-ntp on"
done
for ORDERER in $ORDERERS; do
	ssh $ORDERER "cd $SRC_DIR && rm -rf $NETWORK_DIR"
	# ssh $ORDERER "sudo timedatectl set-ntp off; sleep 1; sudo timedatectl set-ntp on"
done
# sudo timedatectl set-ntp off; sleep 1; sudo timedatectl set-ntp on
rm -rf $NETWORK_DIR
python prepare.py $NETWORK_DIR $CHANNEL $BLOCK_SIZE $ORDERER_COUNT $ORDERERS $PEER_COUNT $PEERS

for PEER in $PEERS; do
	scp -r $NETWORK_DIR $PEER:$SRC_DIR/ > /dev/null 2>&1
done
for ORDERER in $ORDERERS; do
	scp -r $NETWORK_DIR $ORDERER:$SRC_DIR/ > /dev/null 2>&1
done

}

function prepare_shared {
	cd $SRC_DIR && rm -rf $NETWORK_DIR
	python prepare.py $NETWORK_DIR $CHANNEL $BLOCK_SIZE $ORDERER_COUNT $ORDERERS $PEER_COUNT $PEERS
}

function deploy {

./setup_channel.sh $NETWORK_DIR $CHANNEL 5

for IDX in `seq ${PEER_COUNT}`; do
   ALL_ORG="$ALL_ORG 'Org${IDX}MSP.peer'"
done

function join_by { local d=$1; shift; local f=$1; shift; printf %s "$f" "${@/#/$d}"; }
#ENDORSE_POLICY="AND($(join_by , $ALL_ORG))"
ENDORSE_POLICY="OR($(join_by , $ALL_ORG))"

for CC_NAME in $MORE_CCS; do
./deployCC.sh $NETWORK_DIR $CHANNEL $CC_NAME "$ENDORSE_POLICY" "InitLedger" "InitLedger"
done

}

if [ -z "$SHARED" ]; then
	prepare
else
	prepare_shared
fi

python network.py $NETWORK_DIR on

sleep 30

deploy

for PEER in $PEERS; do
	PORT=8800
	ssh -f $PEER "PATH=$NODEJS_DIR:$PATH && cd $SRC_DIR && nohup node blockbench/block-server.js $NETWORK_DIR $ORG_ID $CHANNEL $PORT > block-server-$PEER.log 2>&1 &"
	PORT=$(($PORT+1))
	for CC_NAME in $MORE_CCS; do		
		ssh -f $PEER "PATH=$NODEJS_DIR:$PATH && cd $SRC_DIR && nohup node blockbench/txn-server.js $NETWORK_DIR $ORG_ID $CHANNEL $CC_NAME $MODE $PORT > txn-server-$PEER-$CC_NAME.log 2>&1 &"
		PORT=$(($PORT+1))
	done	
done
