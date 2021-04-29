#!/bin/bash

# set -x

if [ $# -lt 1 ]; then
	echo "Usage: $0 <logs dir>"
	exit 1
fi
LOGDIR=$1
OFILE="data-$LOGDIR"

. ./env.sh

echo "# Nodes; Request Rate [tps]; Throughput [tps]; Duration [s]; Block Height; Peak Power [W]; Avg Power [W]" > $OFILE

for TXR in $TXRATES; do
	./parse-fabric-one.sh $LOGDIR/logs-$TXR > tmp.txt
	NODES=`cat tmp.txt | grep Nodes | cut -d ' ' -f 2`
	TH=`cat tmp.txt | grep throughput | cut -d ' ' -f 3`
	BH=`cat tmp.txt | grep blk | cut -d ' ' -f 3`
	T=`cat tmp.txt | grep Duration | cut -d ' ' -f 3`
	POWER_FILE="$LOGDIR/power-$TXR.log"
	if ! [ -f "$POWER_FILE" ]; then
		POWER_FILE="$LOGDIR/logs-$TXR/power-$TXR.log"
	fi
	python get-power.py $POWER_FILE > tmp.txt
	PP=`cat tmp.txt | cut -d ' ' -f 5`
	AP=`cat tmp.txt | cut -d ' ' -f 7`
	echo "$NODES;$TXR;$TH;$T;$BH;$PP;$AP" >> $OFILE
done
