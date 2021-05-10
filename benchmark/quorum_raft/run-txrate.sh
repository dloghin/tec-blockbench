#!/bin/bash

set -x

PEER_COUNT=8

TSTAMP=`date +%F-%H-%M-%S`
LOGDIR="quorum-raft-$PEER_COUNT-logs-$TSTAMP"

# Power 
#POWER_METER_iCMD="rdserial /dev/ttyS0"
#POWER_METER_APP="rdserial"

# dstat
DSTAT="dstat"

HOSTS=`cat hosts | tr '\n' ' '`
# If running from remote, define HEAD_NODE
# HEAD_NODE=
RDIR="git/blockbench/benchmark/quorum_raft"

# TXRATES="1 5 10 15 20 25 30 35 40 45 50 55 60"
# TXRATES="70 80 90 100 110 120 130 140 150 175 200 225 250"
TXRATES="1 5 10 15 20 25 30 35 40 45 50 55 60 70 80 90 100 110 120 130 140 150 175 200 225 250"
# TXRATES="10"

mkdir $LOGDIR
for TXR in $TXRATES; do
	# Start dstat
	if ! [ -z "$DSTAT" ]; then
		for HOST in $HOSTS; do
			ssh $HOST "rm -f dstat.log"
			ssh $HOST "dstat -t -c -d --disk-tps -m -n --integer --noheader --nocolor --output dstat.log > /dev/null" &
		done
	fi
	# Start power meter
	if ! [ -z "$POWER_METER_CMD" ]; then
		$POWER_METER_CMD | tee -a $LOGDIR/power-$TXR.log > /dev/null &
		sleep 1
	fi
	# Run benchmark
	if [ -z "$HEAD_NODE" ]; then
		./run-bench.sh $PEER_COUNT 1 $PEER_COUNT $TXR
	else
		ssh $HEAD_NODE "cd $RDIR && ./run-bench.sh $PEER_COUNT 1 $PEER_COUNT $TXR"
	fi
	# Stop power meter
	if ! [ -z "$POWER_METER_CMD" ]; then
		killall -SIGINT $POWER_METER_APP
		sleep 1
	fi
	if ! [ -z "$DSTAT" ]; then
	        mkdir -p $LOGDIR/dstat-$TXR
		for HOST in $HOSTS; do
			ssh $HOST "killall -9 dstat"
			scp $HOST:dstat.log $LOGDIR/dstat-$TXR/dstat-$TXR-$idx.log
		done
	fi
	if [ -z "$HEAD_NODE" ]; then
		mv all-logs $LOGDIR/logs-$TXR
	else
		scp -r $HEAD_NODE:$RDIR/all-logs $LOGDIR/logs-$TXR
	fi
done
