#!/bin/bash

set -x

# Edit below for your settings
PEER_COUNT=6
TXRATES="5 10 15 20 25 30 35 40 45 50 55 60 65 70 75 80 85 90 95 100"
DRIVER="/home/ubuntu/git/blockbench/src/macro/kvstore/driver"
WORKLOAD="/home/ubuntu/git/blockbench/src/macro/kvstore/workloads/workloada.spec"

# Do not edit the next two lines
TSTAMP=`date +%F-%H-%M-%S`
LOGDIR="fabric-${PEER_COUNT}-logs-$TSTAMP"

# start
mkdir $LOGDIR
cd $LOGDIR
for TXR in $TXRATES; do
	for idx in `seq 1 $PEER_COUNT`; do
		HOST=`printf "jetsontxx%02d.master" $idx`		
		ssh $HOST "rm -f dstat.log"
		ssh $HOST "dstat -t -c -d --disk-tps -m -n --integer --noheader --nocolor --output dstat.log > /dev/null" &
	done
	# Comment below if you dont have power meter
	rdserial /dev/ttyS0 | tee -a power-$TXR.log > /dev/null &
	sleep 1
	mkdir logs-$TXR
	cd logs-$TXR
	for idx in `seq 1 $PEER_COUNT`; do
                HOST=`printf "jetsontxx%02d.master" $idx`
		$DRIVER -db fabric-v2.2 -threads 1 -P $WORKLOAD -txrate $TXR -endpoint $HOST:8800,$HOST:8801 -wl ycsb -wt 20 > client-$HOST.log 2>&1 &
	done
	sleep 120
	killall -9 driver
	# Comment below if you dont have power meter
	killall -SIGINT rdserial
	cd ..
	mkdir dstat-$TXR
	for idx in `seq 1 $PEER_COUNT`; do
		HOST=`printf "jetsontxx%02d.master" $idx`
		ssh $HOST "killall -SIGINT dstat"
		scp $HOST:dstat.log dstat-$TXR/dstat-$TXR-$idx.log
	done
done
cd ..
