# Time-Energy-Cost Benchmarking of Blockchain Systems 

This repository is based on [**BlockBench**](https://github.com/ooibc88/blockbench), the first benchmarking framework for permissioned blockchain systems. Different from the original BlockBench, this repository:

- adds benchmarking support for Fabric 2+ (e.g., v2.3.1) and Quorum (e.g., v20.10.0).
- adds system-level benchmarks (in [benchmark/systems_benchmarks](benchmark/systems_benchmarks))
- adds scripts to do profiling (with Linux ``perf``)
- adds scripts to capture and parse power and energy

## Experiments Reproducibility

Please see [Benchmarking.md](Benchmarking.md) on how to reproduce the experiments presented in the paper *"Characterizing the Performance and Cost of Blockchains on the Cloud and at the Edge"*.

## Running Fabric v2+ Benchmarks

Please see the [README](benchmark/fabric2/README.md).

## Running Quorum Benchmarks

Please see the [README](benchmark/quorum_raft/README.md) for Quorum Raft.

Please see the [README](benchmark/quorum_ibft/README.md) for Quorum IBFT.

## Running Systems Benchmarks

Please see the [README](benchmark/systems_benchmarks/README.md) for systems benchmarks.

# Other Information

## Workloads 

### Macro-benchmark

* YCSB (KVStore).
* SmallBank (OLTP).

### Micro-benchmark

* DoNothing (consensus layer).
* IOHeavy (data model layer, read/write oriented).
* Analytics (data model layer, analytical query oriented).
* CPUHeavy (execution layer).

## Source file structure

+ Smart contract sources are in [benchmark/contracts](benchmark/contracts) directory.
+ Instructions and scripts to run benchmarks for Ethereum, Hyperledger , Parity and Quorum are in [ethereum](benchmark/ethereum),
[hyperledger](benchmark/hyperledger) , [parity](benchmark/parity) , [quorum_raft](benchmark/quorum_raft) and [quorum_vote](benchmark/quorum_vote) directories respectively.
+ Drivers for benchmark workloads are in [src](src) directory.

## Dependency

### C++ libraries
* [restclient-cpp](https://github.com/mrtazz/restclient-cpp)

  Note: we patched this library to include the "Expect: " header in POST requests, which considerably improves the speed for
  processing RPC request at Parity. 

    + The patch file is include in [benchmark/parity](benchmark/parity) folder.
    + To patch: go to top-level directory of restclient-cpp, then:

        `patch -p4 < $BLOCK_BENCH_HOME/benchmark/parity/patch_restclient`

    + The installation can then proceed as normal. 

* [libcurl](https://curl.haxx.se/libcurl/)

### Node.js libraries
Go to [micro](src/micro) directory and use `npm install` to install the dependency libraries
* [Web3.js](https://github.com/ethereum/web3.js/)
* [zipfian](https://www.npmjs.com/package/zipfian)
* [bignumber.js](https://www.npmjs.com/package/bignumber.js)

### Blockchain 
* [geth(ethereum)](https://github.com/ethereum/go-ethereum/wiki/Installation-Instructions-for-Ubuntu)
* [geth(parity)](https://github.com/paritytech/parity/wiki/Setup)
* [geth(quorum)](https://github.com/jpmorganchase/quorum/wiki/Getting-Set-Up)
* [hyperledger](https://github.com/hyperledger/fabric/tree/v0.6)

## Issues

For any issues, please open an issue in Github [here](https://github.com/dloghin/tec-blockbench/issues).