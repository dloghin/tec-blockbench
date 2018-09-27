#!/bin/bash

if [ $# -lt 1 ]; then
	echo "Usage: $0 <size>"
	exit 1
fi
SIZE=$1

echo "Create and Sort $SIZE values"
/usr/bin/time curl -X PUT localhost:10012/api/sorter/sorter-create-and-sort?size=$SIZE\&partyName=PartyA
