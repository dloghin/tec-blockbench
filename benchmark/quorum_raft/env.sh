QUO_HOME=/git/blockbench/benchmark/quorum_raft
QUO_DATA=/git/blockbench/benchmark/quorum_raft/data
GO_PATH=/go
BENCHMARK=ycsb
EXE_HOME=$QUO_HOME/../../src/macro/kvstore
# BENCHMARK=smallbank
# EXE_HOME=$QUO_HOME/../../src/macro/smallbank

#####################################################
HOSTS=$QUO_HOME/hosts
CLIENTS=$QUO_HOME/clients
QUORUM=/git/quorum/build/bin/geth
ADDRESSES=$QUO_HOME/addresses
LOG_DIR=$QUO_HOME/logs/${BENCHMARK}results_1
