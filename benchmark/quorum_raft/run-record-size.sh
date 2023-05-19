#!/bin/bash

. ./env.sh

SIZES="1 4 6 8 10 12"

for SIZE in $SIZES; do
	cp template-workloada.txt $WORKLOAD
	echo "fieldcount=$SIZE" >> $WORKLOAD
	./run-txrate.sh
done
