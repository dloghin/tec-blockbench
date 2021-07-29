#!/bin/bash
cd `dirname ${BASH_SOURCE-$0}`
. env.sh

rm -rf $LOG_DIR
mkdir -p $LOG_DIR

echo "[*] start-mining.sh"

let i=$1+1

ARGS="--allow-insecure-unlock --nodiscover --istanbul.blockperiod 2 --istanbul.requesttimeout 100000 --networkid 17 --syncmode "full" --mine --minerthreads 1 --rpc --rpcaddr 0.0.0.0 --rpcapi admin,db,eth,debug,miner,net,shh,txpool,personal,web3,quorum,istanbul"

# For perf profiling, uncomment below
# PRIVATE_CONFIG=ignore nohup perf record -a -g -o $LOG_DIR/perf-stat-$i.data ${QUORUM} --datadir $QUO_DATA/dd$i $ARGS --rpcport 8000 --port 9000 --unlock 0 --password <(echo -n "") > $LOG_DIR/server_$i 2>&1 &
# PRIVATE_CONFIG=ignore nohup perf stat -a -o $LOG_DIR/perf-stat-$i.txt ${QUORUM} --datadir $QUO_DATA/dd$i $ARGS --rpcport 8000 --port 9000 --unlock 0 --password <(echo -n "") > $LOG_DIR/server_$i 2>&1 &

PRIVATE_CONFIG=ignore nohup ${QUORUM} --datadir $QUO_DATA/dd$i $ARGS --rpcport 8000 --port 9000 --unlock 0 --password <(echo -n "") > $LOG_DIR/server_$i 2>&1 &
# echo --datadir $QUO_DATA --rpc --rpcaddr 0.0.0.0 --rpcport 8000 --port 9000 --raft --raftport 50400 --raftblocktime 2000 --unlock 0 --password <(echo -n "") 
# echo "[*] miner started"i
sleep 1

