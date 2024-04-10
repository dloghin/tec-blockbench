#!/bin/bash

BENCHS="CreateTask SetPartialParamsHash SetPartialModelHash GetInitialModelHash GetPartialModelHash GetPartialParamsHash"
BEND="quorum"
NTH=2

if [ $# -lt 1 ]; then
    echo "Usage: $0 <logs-dir>"
    exit 1
fi

LOGSD=$1
for BENCH in $BENCHS; do
    TH=`cat $LOGSD/log-$BEND-$BENCH-$NTH.txt | grep "Duration" | cut -d ' ' -f 7`
    echo -n "$TH;"
done
echo "$LOGSD"
