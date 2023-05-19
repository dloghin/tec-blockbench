This page explains how to reproduce the experiments presented in the paper *"Characterizing the Performance and Cost of Blockchains on the Cloud and at the Edge"*.

All the experiments are run on Ubuntu 18.04 LTS operating system.

## Pre-requisites

go 1.15.6 (get it from [here](https://go.dev/dl/go1.15.6.linux-amd64.tar.gz)).

gcc, make, Docker, ssh

## System Level

The experiments in this section are executed on a single node (except for Networking which requires two nodes).

### CPU - CoreMark

Results reported in Table 2 and Figure 5.

Download CoreMark from [here](https://github.com/eembc/coremark) and compile it. You should get an executable named ``coremark.exe``.

Run on one core:

```
cd benchmark/systems_benchmarks/cpu
./run-coremark.sh 1
```

Run on all cores (N):

```
cd benchmark/systems_benchmarks/cpu
./run-coremark.sh $N
```

### CPU - secp256k1 and secp256r1

Results reported in Figure 6.

```
cd benchmark/systems_benchmarks/cpu/secp256k1
go mod init secp256k1
go mod tidy
go build
./secp256k1 64
```

```
cd benchmark/systems_benchmarks/cpu/secp256r1
go mod init secp256r1
go mod tidy
go build
./secp256r1 sign 64
```

### Power

Results reported in Table 2.

For idle power, do not run any process. Monitor the power for 1-2 minutes.

For other benchmarks, start the power measurements before running the benchmark and stop the measurements at the end.


### Memory - lmbench

Results reported in Figure 7.

Download lmbench from [here](https://sourceforge.net/projects/lmbench/) and compile. Note the path to the ``bw_mem`` executable and update the ``EXEC`` variable in ``run-lmbench.sh``. Then run the script: 

```
cd benchmark/systems_benchmarks/memory
./run-lmbench.sh
```

### Storage

Results reported in Table 2.

Make sure you have ``ioping`` installed in Ubuntu:

```
sudo apt install ioping
```

Then run:

```
cd benchmark/systems_benchmarks/storage
sudo ./storage-bench.sh
```

### Networking

Results reported in Table 2.

Make sure you have ``iperf`` installed in Ubuntu:

```
sudo apt install iperf
```

You need two nodes. Please modify the IP addresses of HOST1 and HOST2 in ``networking-bench.sh`` accordingly.

```
cd benchmark/systems_benchmarks/networking
./networking-bench.sh
```

### Profiling with perf

For profiling with ``perf`` we mainly use the ``perf stat`` and ``perf record`` options. You can read more about ``perf`` [here](https://perf.wiki.kernel.org/index.php/Main_Page).

Make sure you have ``perf`` installed on each node. You need to install the ``linux-tools-xxx-generic`` corresponding to your kernel.

## Blockchain Level

Blockchain benchmarks are run on 4, 6, 8, and 10 nodes. In addition, we use up to 10 client nodes that send the transactions. All the nodes, including the clients, must have this repository. A diagram of our setup is presented in Figure 3 of the paper.

Set up passwordless SSH among the nodes. For example, see [this tutorial](https://www.redhat.com/sysadmin/passwordless-ssh).

### Blockchain Level Pre-requisites

After you clone this repository (on each node), do the following:

(1) Build the KVStore benchmark driver (used by the clients)

```
cd src/macro/kvstore
make
```

This will produce an executable called ``driver``.

(2) Build Fabric v2.3.1

```
git clone https://github.com/hyperledger/fabric.git
cd fabric
git checkout v2.3.1
make clean
make native
make docker
```

The executables are in ``build/bin/``

(3) Build Quorum v20.10.0

```
git clone https://github.com/ConsenSys/quorum.git
cd quorum
git checkout v20.10.0
make clean
make
```

The executable is in ``build/bin/geth``.

### Throughput, Latency, Power

Results presented in Figure 4.

#### Fabric

Use ``benchmark/fabric2`` folder of this repo:

```
cd benchmark/fabric2
```

(1) Copy Fabric binaries from Fabric's repo (``build/bin/``) to a folder called ``bin`` under ``benchmark/fabric2``.

(2) Change [env.sh](benchmark/fabric2/env.sh) to match your setup. For example, set ``PEER_COUNT`` and ``ORDERER_COUNT`` to change the number of peers and orderers. Also, provide their IP addresses in ``PEERS`` and ``ORDERERS``.

(3) Run [setup_all.sh](benchmark/fabric2/setup_all.sh) to start Fabric. You may also change the endorsement policy by setting ``ENDORSE_POLICY=...``.

(4) Run benchmark with [run-fabric.sh](benchmark/fabric2/run-fabric.sh).

We recommend a calibration step to determine the best transaction rate. You can set multiple transaction rates in [env.sh](benchmark/fabric2/env.sh), variable ``TXRATES``. Once you determine the best rate, repeat the experiments on 4, 6, 8, and 10 nodes. Use the script ``parse-fabric-one.sh`` to parse the output logs of one run and get the throughput and latency. To get the power, use the script ``get-power.py``.


#### Quorum Raft

Use ``benchmark/quorum_raft`` folder of this repo:

```
cd benchmark/quorum_raft``
```

(1) Define the IP addresses of hosts (peers) and the clients in the [hosts](benchmark/quorum_raft/hosts) and [clients](benchmark/quorum_raft/clients) files, respectively. If using a single client node to send all the requests, you need to repeat the address in the [clients](benchmark/quorum_raft/clients) file to match the number of hosts.

(2) Change [env.sh](benchmark/quorum_raft/env.sh) to match your setup. ``QUO_HOME`` should be the current folder. ``QUORUM`` is the path to quorum binary.

(3) Run [replace-ip.sh](benchmark/quorum_raft/replace-ip.sh) and [send-config.sh](benchmark/quorum_raft/send-config.sh) to prepare the peer nodes (the parameter is the number of peers you are going to use):

```
$ ./replace-ip.sh raft/static-nodes<N>.json
$ ./send-config.sh 4
```

(4) Run the benchmark. We also recommend a calibration step to determine the best request rate. For this, set the ``TXRATES`` variable in ``run-txrate.sh`` and run the script. Once you determine the best transaction rate, repeat the benchmarking steps for 4, 6, 8, and 10 nodes.

Use ``parse-txrate-one.sh`` to get the throughput and latency of one run of the benchmark. To get the power, use the script ``get-power.py``.

#### Quorum IBFT

Use ``benchmark/quorum_ibft`` folder of this repo:

```
cd benchmark/quorum_ibft``
```

Next, repeat the same steps as for Quorum Raft above.


### Effect of networking bandwidth on Fabric and Quorum

Results presented in Figures 8 and 10.

(1) To limit the networking bandwidth on a node, use ``tc``. E.g., to limit the bandwidth to 200Mbps:

```
$ sudo tc qdisc add dev ens5 root tbf rate 200mbit burst 2mbit latency 400ms
```

(2) Run the respective blockchain benchmarking as shown for Fabric and Quorum above. 

(3) Remove the limit:

```
$ sudo tc qdisc del dev ens5 root
```

Repeat steps (1)-(3) for bandwidths in the list [100Mbps, 200Mbps, 500Mbps, 1Gbps 10Gbps].
  

### Effect of the number of orderers in Fabric

Results presented in Figure 9.

For this, please modify the number of orderers and their IP addresses in [env.sh](benchmark/fabric2/env.sh) under ``benchmark/fabric2``. For each distinct number of orderers, run Fabric benchmarking (``run-fabric.sh``).


### Effect of YCSB record size on smart contract execution

Results presented in Figure 11.

We use 4 nodes (peers) for this experiment.

(1) Apply the patches for [Fabric](benchmark/fabric2/patch-chaincode-time-fabric2.3.1.patch) and [Quorum](benchmark/quorum_raft/patch-sc-time-quorum20.10.0.patch) to their source codes and re-compile them (see section Blockchain Level Pre-requisites above).

(2) Run ``run-record-size.sh`` script in ``fabric2`` and ``quorum_raft``.

(3) Use ``parse-chaincode-execution-time.sh`` and ``parse-evm-execution-time.sh`` to get the execution times of the execution engine in Fabric and Quorum, respectively.


### Effect of endorsement policy on Fabric

Results presented in Figure 12.

Please change the endorsement policy by setting ``ENDORSE_POLICY=...`` in [setup_all.sh](benchmark/fabric2/setup_all.sh). Then start Fabric and repeat the benchmarking on 4, 6, 8, and 10 peers.
 

### Profiling

Results presented in Listings 1 and 2.

We use ``perf record`` and then ``perf report``. Run it as ``perf record -a -g`` on each node. For Fabric, you can uncomment the perf profiling in 	``run-fabric.sh``. For Quorum, you can remove the comment of the ``perf record`` line in [start-mining.sh](start-mining.sh).

## Issues

For any issues, please open an issue in Github [here](https://github.com/dloghin/tec-blockbench/issues).