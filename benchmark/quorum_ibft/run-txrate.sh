#!/bin/bash

set -x

PEER_COUNT=10

TSTAMP=`date +%F-%H-%M-%S`
LOGDIR="quorum-ibft-$PEER_COUNT-logs-$TSTAMP"

RDIR="git/blockbench/benchmark/quorum_ibft"

# TXRATES="1 5 10 15 20 25 30 35 40 45 50 55 60"
# TXRATES="70 80 90 100 110 120 130 140 150 175 200 225 250"
TXRATES="1 5 10 15 20 25 30 35 40 45 50 55 60 70 80 90 100 110 120 130 140 150 175 200 225 250"

mkdir $LOGDIR
#cd $LOGDIR
for TXR in $TXRATES; do
	./run-bench.sh $PEER_COUNT 1 $PEER_COUNT $TXR
#        for idx in `seq 1 10`; do
#                HOST=`printf "jetsontxx%02d.master" $idx`
#               ssh $HOST "rm -f dstat.log"
#                ssh $HOST "dstat -t -c -d --disk-tps -m -n --integer --noheader --nocolor --output dstat.log > /dev/null" &
#        done
#        rdserial /dev/ttyS0 | tee -a power-$TXR.log > /dev/null &
#        sleep 1
#        ssh jetsontxx08.master "cd $RDIR && ./run-bench.sh 10 1 10 $TXR"
#        killall -SIGINT rdserial
#        mkdir dstat-$TXR
#        for idx in `seq 1 10`; do
#                HOST=`printf "jetsontxx%02d.master" $idx`
#                ssh $HOST "killall -9 dstat"
#                scp $HOST:dstat.log dstat-$TXR/dstat-$TXR-$idx.log
#        done
#        scp -r jetsontxx08.master:$RDIR/all-logs .
        mv all-logs $LOGDIR/logs-$TXR
done

