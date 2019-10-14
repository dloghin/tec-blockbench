#!/bin/bash
# args=THREADS index N txrate
echo "[*] start-clients" $1 $2 $3 $4

cd `dirname ${BASH_SOURCE-$0}`
. env.sh

#..
export CPATH=/users/dinhtta/local/include
export LIBRARY_PATH=/users/dinhtta/local/lib:$LIBRARY_PATH
export LD_LIBRARY_PATH=/users/dinhtta/local/lib:$LD_LIBRARY_PATH
#..

#LOG_DIR=$ETH_HOME/../src/ycsb/exp_$3"_"servers_run4
LOG_DIR=$LOG_DIR/exp_$3"_"servers_$1"_"threads_$4"_"rates
mkdir -p $LOG_DIR
i=0
for host in `cat $HOSTS`; do
  let n=i/2
  let i=i+1
  if [[ $n -eq $2 ]]; then
    cd $EXE_HOME
    #both ycsbc and smallbank use the same driver
    #-P workloads/workloada.spec for smallbank
    if [[ $BENCHMARK == 'ycsb' ]]; then
      nohup ./driver -db ethereum -threads $1 -P workloads/workloadb.spec -endpoint $host:8000 -txrate $4 -wl ycsb -wt 120 > $LOG_DIR/client_$host"_"$1 2>&1 &
    elif [[ $BENCHMARK == 'smallbank' ]]; then
      nohup ./driver -db ethereum -ops 1000000 -threads $1 -txrate $4 -fp $LOG_DIR/stat.log -endpoint $host:8000 > $LOG_DIR/client_$host"_"$1 2>&1 &
    elif [[ $BENCHMARK == 'donothing' ]]; then
      nohup ./driver -db ethereum -threads $1 -P workloads/workloadb.spec -endpoint $host:8000 -txrate $4 -wl donothing > $LOG_DIR/client_$host"_"$1 2>&1 &
    fi
  fi
done

