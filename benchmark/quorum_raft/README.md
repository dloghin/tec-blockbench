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
$ cd blockbench/benchmark/quorum_raft
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
$ cd ~/git/blockbench/benchmark/quorum_raft
$ ./run-txrate.sh
```

After running, you will get a folder with a timestamp suffix, such as "quorum-raft-4-logs-2021-05-06-06-35-42". You can use [parse-txrate-all.sh](parse-txrate-all.sh) to parse it, but make sure the TXRATES defined in [run-txrate.sh](run-txrate.sh) is the same as the TXRATES in [parse-txrate-all.sh](parse-txrate-all.sh):

```
$ ./parse-txrate-all.sh quorum-raft-4-logs-2021-05-06-06-35-42
$ cat data-quorum-raft-4-logs-2021-05-06-06-35-42
# Nodes; Request Rate [tps]; Throughput [tps]; Duration [s]; Block Height; Peak Power [W]; Avg Power [W]; Latency [s]
10;1;6.22352941176470588235;340;108;0.0;0.0;12.00090555028462998102
10;5;30.48823529411764705882;340;110;0.0;0.0;12.21233730810286268801
10;10;59.33235294117647058823;340;109;0.0;0.0;12.63181380800953989863
10;15;86.87647058823529411764;340;109;0.0;0.0;13.01713436989657049537
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


# Detailed Information

## Parameters
There are a number of global variables that are to be set in `env.sh`:
+ `$QUO_HOME`: NFS-mounted directory containing the scripts.
+ `$QUO_DATA`: non-NFS directory for Quorum data
+ `SHOSTS`: contains IP addresses of all nodes used for running the miners. The
example file contains 32 hosts 
+ `$CLIENTS`: containing IP addresses of all nodes used by the clients. There are 2 clients per
node. The example file contains 16 hosts
+ `$LOG_DIR`: directory where the logs are stored. NFS-mounted directories are recommended.  
+ `$EXE_HOME`: containing the executable for driving the workloads (ycsb or smallbank). For Quorum, smallbank
is simulated via ycsb, i.e. it is invoked via key-value operation but at the server side multiple read/write
operations are performed. 
    + YCSB: `$QUO_HOME/../../src/kystore`
    + Smallbank: `$QUO_HOME/../../src/smallbank`
+ `$BENCHMARK`: name of the benchmark (ycsb or smallbank)
+ `$QUORUM`: the directory of Quorum Binaries, which are placed in $QUORUM_REPO_ROOT/build/bin

Each network is initialized with different genesis block. There are a set of pre-defined genesis blocks for
different network sizes:

## Scripts
There are 4 steps in running an experiment: network initialization, miner startup, client startup, and finally
cleaning up. Scripts for these steps are included in `$QUO_HOME`. The top-level script:

    run-bench.sh <nservers> <nthreads> <nclients> [-drop]

will start `nservers` miners, `nclients` clients each running with `nthreads`. The outputs are stored in
`$LOG_DIR`. If `-drop` is specified, 4 servers will be killed after the clients are running for about 250s.  

### Initilization
+ `init-all.sh <nservers>`: go to each of `nservers` and invokes `init.sh <nservers>`
+ `init.sh <nservers>` initializes `quorum` at a local node. It does 4 things:
    + Use `keys/key$i` for the key of the static account
    + Use `raft/static-nodes$2.json` and `raft/nodekey$i` for the connect of the static enode
    + Use `$QUO_HOME/genesis_quorum.json` to as the genesis block
    + Use `$QUO_DATA` for `quorum` data 


### Starting miners
+ `start-all.sh <nservers>`: start a network of `nservers` miner in the following steps:
    1. Go to each of `nservers` node in `hosts` and invoke `start-mining.sh`. This involves:
        + Start `quorum` in raft mode

### Starting the clients
+ `start-multi-clients.sh <nclients> <nservers> <nthreads> [-drop]`: takes as input a number of clients and
launch `2.nclients` clients connecting to `2.nclients` miners. It then performs the following:
    1. Go to each client node in `clients` and invoke `start-client.sh` 
        + `start-client.sh <nthreads> <client_index> <nservers>`: start `driver` process at the client node
       `client_index^{th}` to connect to the server node of index `2.(client_index)` and `2.(client_index)+1`
       
       **Different benchmark may expect different command line arguments for the `driver` process**

    2. Let the clients run for `M` (seconds), a sufficiently long duration to collect enough data. Then kill
    all client processes. Particularly:

        `M = 240 + 10*<nservers>`
    3. When `-drop` is specified:
        + It starts the clients and sleep for 250 seconds 
        + It then kills off the *last* 4 servers
        + It then continues to run (the remaining clients and servers) for another `(M-150)` seconds. 

    So in total, when `-drop` is specified, the entire experiments runs for about `(M+100)` seconds. 

### Cleaning up
+ `stop-all.sh <nservers>`: kill all server and client processes. Particularly:
    1. Go to each server and invoke `stop.sh` which kills `quorum` and remove all Quorum data. 
    2. Go to each client (in `$QUO_HOME/clients`) and kill the `ycsbc` process. 

When the experiment exits cleanly (normal case), the client processes are already killed in
`start-multi-client.sh` and server processes in `run-bench.sh`. But if interrupted (Ctrl-C), both server and client processes should be killed
explicitly with `stop-all.sh`.


## Examples

1. Running with the same number of clients and servers: 
    + Change `LOG_DIR` in `env.sh` to correct location, say `result_same_s_same_c`
    + Start it (e.g. X=8): 

        `. run-bench.sh 8 16 8 16`

    This will start 8 miners (on first 8 nodes in `$HOSTS`) and 8 clients (on 4 first 4 nodes in `$CLIENTS`).
    Each client runs `driver` process with 16 threads. The clients output logs to
    `result_same_s_same_c/exp_8_servers` directory, with the file format `client_<miner_host>_16`.   

2. Running with fixed number of clients and varying number of servers: 
    + Change `LOG_DIR` in `env.sh` to correct location, say `result_fixed_c`
    + Start it (X=16): 

        `. run-bench.sh 16 16 8 16`

    This will start 16 miners (on first 16 nodes in `$HOSTS`) and 8 clients (on 4 first 4 nodes in
    `$CLIENTS`). Each client runs `driver` process with 16 threads. The clients output logs to
    `result_fixed_c/exp_16_servers` directory, with the file format `client_<miner_host>_16`.   


3. Drop off nodes:
    + Change `LOG_DIR` in `env.sh` to correct location, say `result_fixed_c_drop_4`
    + Start it (X=16): 

        `. run-bench.sh 16 16 8 16 -drop`

    This will start 16 miners (on first 16 nodes in `$HOSTS`) and 8 clients (on 4 first 4 nodes in
    `$CLIENTS`). Each client runs `driver` process with 16 threads. At 250th second, 4 miners are killed off,
    while the rest continues to run. The clients output logs to `result_fixed_c_drop_4/exp_16_servers`
      directory, with the file format `client_<miner_host>_16`.   

4. Running with different workloads:

Simply change `$EXE_HOME` and `$BENCHMARK` variables in `env.sh`. Then repeat the same steps as in the other
examples. 

