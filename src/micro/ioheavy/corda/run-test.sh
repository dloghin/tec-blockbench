#!/bin/bash

SIZE=100

echo "1. Write"
curl -X PUT localhost:10012/api/ioheavy/ioheavy?command=1\&starterKey=1\&numKeys=100\&partyName=PartyA

echo ""
echo "2. Scan"
curl -X PUT localhost:10012/api/ioheavy/ioheavy?command=2\&starterKey=2\&numKeys=100\&partyName=PartyA

