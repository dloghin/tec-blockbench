#!/bin/bash
#nodes
cd `dirname ${BASH_SOURCE-$0}`
. env.sh

echo "start-all.sh"

i=0
for host in `cat $HOSTS`; do
  if [[ $i -lt $1 ]]; then
    echo [*] Starting Ethereum node $i on host: $host
    ssh -oStrictHostKeyChecking=no $host $QUO_HOME/start-mining.sh $i
  fi
  let i=$i+1
done

echo done start-all.sh
