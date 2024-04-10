#!/bin/bash

set -x

. ./env.sh

for PEER in $PEERS; do
	PIDS=`ssh $PEER "ps aux" | grep "node blockbench" | tr -s ' ' | cut -d ' ' -f 2`
	for PID in $PIDS; do
		ssh $PEER "kill -9 $PID"
	done
done
sleep 2
for PEER in $PEERS; do
	PORT=8800
	ssh -f $PEER "PATH=$NODEJS_DIR:$PATH && cd $SRC_DIR && nohup node blockbench/block-server.js $NETWORK_DIR $ORG_ID $CHANNEL $PORT > block-server-$PEER.log 2>&1 &"
	PORT=$(($PORT+1))
	for CC_NAME in $MORE_CCS; do		
		ssh -f $PEER "PATH=$NODEJS_DIR:$PATH && cd $SRC_DIR && nohup node blockbench/txn-server.js $NETWORK_DIR $ORG_ID $CHANNEL $CC_NAME $MODE $PORT > txn-server-$PEER-$CC_NAME.log 2>&1 &"
		PORT=$(($PORT+1))
	done	
done