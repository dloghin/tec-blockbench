#!/bin/bash

echo "1. Deploy"
curl -X PUT localhost:10012/api/analytic/deploy?accounts=10\&transactions=10\&partyName=PartyA


