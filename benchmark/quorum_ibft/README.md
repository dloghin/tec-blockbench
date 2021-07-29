# Benchmarking Guide for Quorum Raft and IBFT

1. Prepare Quorum, by compiling version 20.10.0 and note the path of the binary. (e.g., on Raspberry Pi: /home/pi/git/quorum/build/bin/geth).

2. Prepare blockbench (see below).

## Prepare blockbench

Suppose there is a ``git`` folder in user's home on all the nodes. For example, if the user is "pi", then there is a foder ``/home/pi/git``.

Clone blockbench in ``git`` and use ``analysis2021`` branch on all the nodes, including the clients nodes:

```
$ cd ~/git
$ git clone https://github.com/dloghin/blockbench.git
$ cd blockbench
$ git checkout analysis2021
$ cd blockbench/benchmark/quorum_ibft
```

Then, on one of the nodes (let's call it HEAD_NODE) which can act as the client that sends transactions, do the following. Define the hosts (peers) and the clients in [hosts](hosts) and [clients](clients) files, respectively. If using a single client node to send all the requests, you need to repeat the address in the [clients](clients) file to match the number of hosts.

Make sure you use passwordless ssh to connect to all the other nodes (add id_rsa.pub in authorized_keys).

Edit [env.sh](env.sh):
- QUO_HOME should be the current folder (e.g., on Raspberry Pi: home/pi/git/blockbench/benchmark/quorum_raft).
- QUORUM is the path to quorum binary (step 1 above).

Run [replace-ip.sh](replace-ip.sh) and [send-config.sh](send-config.sh) to prepare the peer nodes (the parameter is the number of peers you are going to use):

```
$ ./replace-ip.sh
$ ./send-config.sh 4
```

This will also create the proper ``static-nodes.json`` files.

Next, edit [run-txrate.sh](run-txrate.sh):
- define PEER_COUNT (e.g. PEER_COUNT=4)
- define POWER_METER_CMD and POWER_METER_APP or comment them if there is no power meter. (POWER_METER_APP is used in killall).
- if you run the benchmark from a machine different from the HEAD_NODE client node (as above), define HEAD_NODE=<addr>. Otherwise comment it.
- define the TXRATES (all the send rates that the benchmark will be executed with).
- make sure you have dstat installed on all the systems.

Next, make sure the kvstore client is compiled on the client node(s):

```
$ cd ~/git/blockbench/src/macro/kvstore
$ make clean
$ make
```

You should get a driver binary.

## Run the benchmark

Now, you are ready to run:

```
$ cd ~/git/blockbench/benchmark/quorum_ibft
$ ./run-txrate.sh
```

After running, you will get a folder with a timestamp suffix, such as "quorum-raft-4-logs-2021-05-06-06-35-42". You can use [parse-txrate-all.sh](parse-txrate-all.sh) to parse it, but make sure the TXRATES defined in [run-txrate.sh](run-txrate.sh) is the same as the TXRATES in [parse-txrate-all.sh](parse-txrate-all.sh):

```
$ ./parse-txrate-all.sh quorum-raft-4-logs-2021-05-06-06-35-42
$ cat data-quorum-raft-4-logs-2021-05-06-06-35-42
# Nodes; Request Rate [tps]; Throughput [tps]; Duration [s]; Block Height; Peak Power [W]; Avg Power [W]; Latency [s]
10;1;1.65882352941176470588;340;6;0.0;0.0;40.96516489361702127659
10;5;8.33529411764705882352;340;6;0.0;0.0;40.31836944248412138320
10;10;17.22058823529411764705;340;6;0.0;0.0;75.86580396475770925110
10;15;16.30882352941176470588;340;6;0.0;0.0;64.76571686203787195671
...
```

IBFT - all the steps for Raft apply for IBFT, just that you need to use the folder: [blockbench/benchmark/quorum_ibft](blockbench/benchmark/quorum_ibft).

## Networking benchmarks

To limit the networking bandwidth on a node, use ``tc``. E.g., to limit the bandwidth to 200Mbps:

```
$ sudo tc qdisc add dev ens5 root tbf rate 200mbit burst 2mbit latency 400ms
```

To remove the limit:

```
$ sudo tc qdisc del dev ens5 root
```

## Perf profiling

To profile the execution of Fabric with ``perf``, make sure you have ``perf`` installed on each node. You need to install the ``linux-tools-xxx-generic`` corresponding to your kernel.

For ``perf record``, run ``perf record -a -g`` on each node. You can remove the comment of the ``perf record`` line in [start-mining.sh](start-mining.sh).

To get the number of CPU cores uses, run with ``perf stat``. For cache misses, we use the following events: ``-e instructions -e LLC-load-misses -e LLC-loads -e LLC-store-misses -e LLC-stores -e cache-misses -e cache-references``.
