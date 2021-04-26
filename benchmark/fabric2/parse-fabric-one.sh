#!/bin/bash

T=120

if [ $# -lt 1 ]; then
        echo "Usage: $0 <log dir>"
        exit 1
fi
if ! [ -d "$1" ]; then
        echo "No such dir $1"
        exit 1
fi
cd $1

CLIENTS=`ls client*`

MAXTXN=0
MAXBLK=0
NC=0
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
done
MAXTH=`echo $MAXTXN/$T | bc -l`

echo "Nodes: $NC"
echo "Duration [s]: $T"
echo "Max txn: $MAXTXN"
echo "Max blk: $MAXBLK"
echo "Max throughput: $MAXTH"
