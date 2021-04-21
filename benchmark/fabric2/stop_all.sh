#!/bin/bash

set -x

. ./env.sh

python network.py test off

for PEER in $PEERS; do
	ssh $PEER "killall -9 node"
done
