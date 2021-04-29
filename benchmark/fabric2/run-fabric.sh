#!/bin/bash

set -x

. ./env.sh

# Do not edit the next two lines
TSTAMP=`date +%F-%H-%M-%S`
LOGDIR="fabric-${PEER_COUNT}-logs-$TSTAMP"

# start
mkdir $LOGDIR
cd $LOGDIR
for TXR in $TXRATES; do
	mkdir logs-$TXR
	cd logs-$TXR
	for PEER in $PEERS; do
		ssh $PEER "killall -9 dstat; sleep 3; rm -f dstat-$PEER.log"
		ssh $PEER "dstat -t -c -d --disk-tps -m -n --integer --noheader --nocolor --output dstat-$PEER.log > /dev/null" &
	done
	if ! [ -z "$POWER_METER_CMD" ]; then
		$POWER_METER_CMD | tee -a power-$TXR.log > /dev/null &
		sleep 1
	fi
	N=91
#	CLIENT="slave-91"
	WDIR=`pwd`
	for PEER in $PEERS; do
#		N=$(($N+1))
#		if [ $N -gt 4 ]; then
#			CLIENT="slave-93"
#		fi
		CLIENT="slave-$N"
#		N=$(($N+1))
		for M in `seq 1 $DRIVERS_PER_CLIENT`; do
			ssh -f $CLIENT "cd $WDIR && $DRIVER -db fabric-v2.2 -threads 1 -P $WORKLOAD -txrate $TXR -endpoint $PEER:8800,$PEER:8801 -wl ycsb -wt 20 > client-$M-$PEER.log 2>&1 &"
		done
	done
	sleep $DURATION
#	killall -9 driver
#	ssh slave-93 "killall -9 driver"
	for N in `seq 91 98`; do
		CLIENT="slave-$N"
		ssh $CLIENT "killall -9 driver"
	done
	# Comment below if you dont have power meter
	if ! [ -z "$POWER_METER_APP" ]; then
		killall -SIGINT $POWER_METER_APP
	fi
	cd ..
	if [ -z "$SHARED" ]; then 
		mkdir dstat-$TXR
		for PEER in $PEERS; do
			ssh $PEER "killall -SIGINT dstat"
			scp $PEER:dstat-$PEER.log dstat-$TXR/dstat-$PEER.log
		done
	fi
done
cd ..
