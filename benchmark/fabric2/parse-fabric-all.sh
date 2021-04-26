#!/bin/bash

# set -x

if [ $# -lt 1 ]; then
	echo "Usage: $0 <logs dir>"
	exit 1
fi
LOGDIR=$1
OFILE="data-$LOGDIR"

TXRATES="5 10 15 20 25 30 35 40 45 50 55 60 65 70 75 80 85 90 95 100"

echo "# Nodes; Request Rate [tps]; Throughput [tps]; Duration [s]; Block Height; Peak Power [W]; Avg Power [W]" > $OFILE

for TXR in $TXRATES; do
	./parse-fabric-one.sh $LOGDIR/logs-$TXR > tmp.txt
	NODES=`cat tmp.txt | grep Nodes | cut -d ' ' -f 2`
	TH=`cat tmp.txt | grep throughput | cut -d ' ' -f 3`
	BH=`cat tmp.txt | grep blk | cut -d ' ' -f 3`
	T=`cat tmp.txt | grep duration | cut -d ' ' -f 4`
	python get-power.py $LOGDIR/power-$TXR.log > tmp.txt
	PP=`cat tmp.txt | cut -d ' ' -f 5`
	AP=`cat tmp.txt | cut -d ' ' -f 7`
	echo "$NODES;$TXR;$TH;$T;$BH;$PP;$AP" >> $OFILE
done
