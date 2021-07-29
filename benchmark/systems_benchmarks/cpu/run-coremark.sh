#!/bin/bash
#
# Run CoreMark on a number of cores.
#
# Copyright (C) 2014-2021 Dumitrel Loghin
#

ARCH=$(uname -m)

BASEDIR=$(dirname $0)

IT=300000

EXEC="$BASEDIR/coremark.exe  0x0 0x0 0x66 $IT 7 1 2000"

if [ $# -lt 1 ]; then
	echo "Usage: $0 <cores>"
	exit 1
fi

cores=$(($1-1))

for core in `seq 0 $cores` ; do
	taskset -c $core $EXEC &
done

wait
