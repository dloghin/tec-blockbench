#!/bin/bash
#
# Memory benchmarking with lmbench.
#
# Copyright (C) 2014-2021 Dumitrel Loghin
#

dateuid=`date +%F-%H-%M-%S`
sys=`cat /etc/hostname`

EXEC="/home/ubuntu/git/scripts/lmbench/bin/bw_mem"
if ! [ -e $EXEC ]; then
	echo "lmbench executable not found: $EXEC"
	exit
fi

# TYPE_LIST="rd wr rdwr cp fwr frd fcp bzero bcopy"
TYPE_LIST="rd wr rdwr"
SIZE_LIST="1k 2k 4k 8k 16k 32k 64k 128k 256k 512k 1m 2m 4m 8m 16m 32m 64m 128m 256m 512m 1024m 2048m 4096m"

FILE_LIST=""
for TYPE in $TYPE_LIST; do
	FILE="lmbench-$sys-$dateuid-$TYPE.csv"
	echo "#Size;ActualSize[MB];BW[MB/s];Type;" > $FILE
	for SIZE in $SIZE_LIST; do
		$EXEC $SIZE $TYPE 2> tmp
		BW=`cat tmp | tr ' ' ';'`
		echo "$SIZE;$BW;$TYPE" >> $FILE
	done
	FILE_LIST="$FILE_LIST $FILE"
done
tar -cjf lmbench-$sys-$dateuid.tar.bz2 $FILE_LIST
rm tmp
