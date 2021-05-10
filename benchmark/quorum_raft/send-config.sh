#!/bin/bash

if [ $# -lt 1 ]; then
	echo "Usage: $0 <# of peers>"
	exit 1
fi

N=$1
DIR=`pwd`
HOSTS=`cat hosts | tr '\n' ' '`
H=`wc -l hosts | cut -d ' ' -f 1`
if [ $H -lt $N ]; then
	echo "Error: only $H peers in hosts ($N required)"
	exit 1
fi
if ! [ -f raft/static-nodes$N.json ]; then
	echo "File not found: raft/static-nodes$N.json"
	exit 1
fi
./replace-ip.sh raft/static-nodes$N.json
for HOST in $HOSTS; do
	scp env.sh clients hosts $HOST:$DIR/
	scp raft/static-nodes$N.json $HOST:$DIR/raft/
done
