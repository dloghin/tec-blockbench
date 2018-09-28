#!/bin/bash

echo "1. Write"
curl -X PUT localhost:10012/api/kvstore/kvstore?command=1\&key=k1\&val=val1\&partyName=PartyA

echo ""
echo "2. Read"
curl -X PUT localhost:10012/api/kvstore/kvstore?command=4\&key=k1\&partyName=PartyA

echo ""
echo "3. Delete"
curl -X PUT localhost:10012/api/kvstore/kvstore?command=2\&key=k1\&partyName=PartyA

echo ""
echo "4. Read again"
curl -X PUT localhost:10012/api/kvstore/kvstore?command=4\&key=k1\&partyName=PartyA
