#!/bin/bash

echo "1. Amalgamate"
curl -X PUT localhost:10012/api/smallbank/smallbank?command=1\&account1=100\&account2=200\&partyName=PartyA

echo ""
echo "2. GetBalance"
curl -X PUT localhost:10012/api/smallbank/smallbank?command=2\&account1=200\&partyName=PartyA

echo ""
echo "3. UpdateBalance"
curl -X PUT localhost:10012/api/smallbank/smallbank?command=3\&account1=100\&amount=999\&partyName=PartyA

echo ""
echo "4. UpdateSaving"
curl -X PUT localhost:10012/api/smallbank/smallbank?command=4\&account1=200\&amount=999\&partyName=PartyA

echo ""
echo "5. SendPayment"
curl -X PUT localhost:10012/api/smallbank/smallbank?command=5\&account1=100\&account2=300\&amount=666\&partyName=PartyA

echo ""
echo "6. WriteCheck"
curl -X PUT localhost:10012/api/smallbank/smallbank?command=6\&account1=100\&amount=99\&partyName=PartyA


