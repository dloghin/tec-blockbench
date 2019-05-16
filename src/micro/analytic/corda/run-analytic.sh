#!/bin/bash

if [ $# -lt 2 ]; then
	echo "Usage: $0 [Q1|Q2|Q3] <size>"
	exit 1
fi
SIZE=$2

if [ "$1" == "Q1" ]; then
    echo "Analytics Q1 $SIZE"
    /usr/bin/time curl -X PUT localhost:10012/api/analytic/analytic?command=1\&startKey=1\&numKeys=$SIZE\&partyName=PartyA
elif [ "$1" == "Q2" ]; then
    echo "Analytics Q2 $SIZE"
    /usr/bin/time curl -X PUT localhost:10012/api/analytic/analytic?command=2\&startKey=1\&numKeys=$SIZE\&partyName=PartyA
elif [ "$1" == "Q3" ]; then
    echo "Analytics Q3 $SIZE"
    /usr/bin/time curl -X PUT localhost:10012/api/analytic/analytic?command=3\&startKey=1\&numKeys=$SIZE\&partyName=PartyA
else
    echo "Invalid operation: $1. Valid operations are Q1, Q2, Q3."
fi
