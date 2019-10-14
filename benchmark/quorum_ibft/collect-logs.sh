#!/bin/bash
#nodes
cd `dirname ${BASH_SOURCE-$0}`
. env.sh

ALL_LOGS="all-logs"
rm -rf $ALL_LOGS
mkdir $ALL_LOGS

i=0
for host in `cat $HOSTS`; do
  if [[ $i -lt $1 ]]; then
    scp -oStrictHostKeyChecking=no $host:$LOG_DIR/server* $ALL_LOGS/
  fi
  let i=$i+1
done
i=0
for host in `cat $CLIENTS`; do
  if [[ $i -lt $2 ]]; then
    scp -oStrictHostKeyChecking=no $host:$LOG_DIR/client* $ALL_LOGS/
  fi
  let i=$i+1
done
