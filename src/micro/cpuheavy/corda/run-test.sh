#!/bin/bash

SIZE=100

echo "1. Create"
curl -X PUT localhost:10012/api/sorter/sorter-create?size=$SIZE\&partyName=PartyA -o tmptx
cat tmptx
UUID=`cat tmptx | cut -d ' ' -f 9`
rm -f tmptx

echo ""
echo "2. Sort"
curl -X PUT localhost:10012/api/sorter/sorter-sort?sorterStateId=$UUID\&partyName=PartyA

echo ""
echo "3. Create and Sort"
curl -X PUT localhost:10012/api/sorter/sorter-create-and-sort?size=$SIZE\&partyName=PartyA
