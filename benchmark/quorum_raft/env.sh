QUO_HOME=/home/ubuntu/git/blockbench/benchmark/quorum_raft
QUO_DATA=/home/ubuntu/git/blockbench/benchmark/quorum_raft/data
GO_PATH=/home/ubuntu/tools/go
BENCHMARK=ycsb
EXE_HOME=$QUO_HOME/../../src/macro/kvstore
# BENCHMARK=smallbank
# EXE_HOME=$QUO_HOME/../../src/macro/smallbank
WORKLOAD="/home/ubuntu/git/blockbench/src/macro/kvstore/workloads/workloada.spec"

#####################################################
HOSTS=$QUO_HOME/hosts
CLIENTS=$QUO_HOME/clients
QUORUM=/home/ubuntu/git/quorum/build/bin/geth
ADDRESSES=$QUO_HOME/addresses
LOG_DIR=$QUO_HOME/logs
