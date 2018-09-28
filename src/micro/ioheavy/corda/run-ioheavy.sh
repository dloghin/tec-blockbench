#!/bin/bash

if [ $# -lt 2 ]; then
	echo "Usage: $0 [W(rite)|S(can)] <size>"
	exit 1
fi
SIZE=$2

if [ "$1" == "W" ]; then
    echo "Write $SIZE key-values"
    /usr/bin/time curl -X PUT localhost:10012/api/ioheavy/ioheavy?command=1\&startKey=1\&numKeys=$SIZE\&partyName=PartyA
elif [ "$1" == "S" ]; then
    echo "Scan $SIZE key-values"
    /usr/bin/time curl -X PUT localhost:10012/api/ioheavy/ioheavy?command=2\&startKey=1\&numKeys=$SIZE\&partyName=PartyA
else
    echo "Invalid operation: $1. Valid operations are W (write) and S (scan)."
fi
