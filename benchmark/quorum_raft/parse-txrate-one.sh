#!/bin/bash

if [ $# -lt 1 ]; then
        echo "Usage: $0 <log dir>"
        exit 1
fi
if ! [ -d "$1" ]; then
        echo "No such dir $1"
        exit 1
fi
cd $1

NODES=`ls server* | wc -l`
CLIENTS=`ls client*`

MAXTXN=0
MAXBLK=0
NC=0
LAT=0
NL=0
for CLIENT in $CLIENTS; do
        TOTTXN=`cat $CLIENT | grep polled | cut -d ' ' -f 5 | tr '\n' '+'`
        TOTTXN=`echo $TOTTXN"0" | bc -l`
        if [ $TOTTXN -gt $MAXTXN ]; then
                MAXTXN=$TOTTXN
        fi
        TOTBLK=`cat $CLIENT | grep polled | cut -d ' ' -f 5 | wc -l`
        if [ $TOTBLK -gt $MAXBLK ]; then
                MAXBLK=$TOTBLK
        fi
        NC=$(($NC+1))
	cat $CLIENT | grep latency | cut -d ' ' -f 11 > tmp.txt
	LATC=`cat tmp.txt | tr '\n' '+'`
	if ! [ -z "$LATC" ]; then
		LAT="$LAT+${LATC}0"
	fi
	NLC=`wc -l tmp.txt | cut -d ' ' -f 1`
	NL=$(($NL+$NLC))
done
T=$(($NC*10+240))
MAXTH=`echo $MAXTXN/$T | bc -l`
LAT=`echo "($LAT+0)/$NL" | bc -l`
rm -f tmp.txt

echo "Nodes: $NODES"
echo "Computed duration (NC*10+240): $T"
echo "Max txn: $MAXTXN"
echo "Max blk: $MAXBLK"
echo "Max throughput: $MAXTH"
echo "Avg Latency: $LAT" 
