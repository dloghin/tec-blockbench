#!/bin/bash

BSETS="CreateTask SetPartialParamsHash SetPartialModelHash"
BGETS="GetInitialModelHash GetPartialModelHash GetPartialParamsHash"

BENDS="fabric"
# BENDS="quorum"

NTH=64
# NTH=2

TSTAMP=`date +%F-%H-%M-%S`
LOGDIR="logs-$TSTAMP"
mkdir $LOGDIR

for BEND in $BENDS; do
    export BENCH_BACKEND="$BEND"
    for BENCH in $BSETS; do
        python bench.py bench_set $BENCH $NTH > $LOGDIR/log-$BEND-$BENCH-$NTH.txt
    done
    for BENCH in $BGETS; do
        python bench.py bench_get $BENCH $NTH > $LOGDIR/log-$BEND-$BENCH-$NTH.txt
    done
done