#!/bin/bash
#
# Storage benchmarking:
# - throughput (dd)
# - latency (ioping)
#
# Copyright (C) 2014-2021 Dumitrel Loghin
#

BS=1M
CNT=2048
TMPF=tempf
TMP1=tmp_1

TSTP=`date +%F-%H-%M-%S`
LOGF="log-storage-$TSTP.txt"
touch $LOGF

function throughput_bench {
	echo "*** running throughput bench using dd version `dd --version | head -1`" | tee -a $LOGF
        # 1. get write speed
        echo "Write" | tee -a $LOGF
        dd if=/dev/zero of=$TMPF bs=$BS count=$CNT conv=fdatasync,notrunc |& tee -a $LOGF

        # 2. get read speed by dropping the OS cache
        echo "Read" | tee -a $LOGF
        echo 1 > $TMP1
        cp $TMP1 /proc/sys/vm/drop_caches
        dd if=$TMPF of=/dev/null bs=$BS count=$CNT |& tee -a $LOGF

        # 3. get buffered read speed
        echo "Buffered Read" | tee -a $LOGF
        dd if=$TMPF of=/dev/null bs=$BS count=$CNT |& tee -a $LOGF

        rm -rf $TMPF
	rm -rf $TMP1
	echo "*** throughput bench done." | tee -a $LOGF
}

function latency_bench {
	echo "*** running latency bench using ioping version `ioping -v`" | tee -a $LOGF
	echo "Write" | tee -a $LOGF
	ioping -c 10 -W . |& tee -a $LOGF
	echo "Read" | tee -a $LOGF
	ioping -c 10 -D . |& tee -a $LOGF
	echo "*** latency bench done." | tee -a $LOGF
}

# check root
if [ $UID -ne 0 ]; then 
	echo "Please run this benchmark as root!"
	exit
fi

#check memory
MEM=`free -m | grep Mem: | tr -s ' ' | cut -d ' ' -f 2`
# set threshhold as 1/3 of the memory
MEM=$((MEM / 3))
if [ $CNT -ge $MEM ]; then
	while [ $CNT -ge $MEM ]; do
		CNT=$(($CNT / 2))
	done
	echo "New dd count value: $CNT" | tee -a $LOGF
fi

throughput_bench
latency_bench
