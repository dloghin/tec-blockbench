# Micro Benchmark Workload - CPUHeavy in Corda
## Usage

To build the source and run 3 nodes and one notary, run:
```
$ ./build-run.sh
```
To issue some transactions, run:
```
$ ./run-test.sh
```
To sort 1000 values in PartyB (port 10012) with counter-party PartyA, run:
```
$ curl -X PUT localhost:10012/api/sorter/sorter-create-and-sort?size=1000\&partyName=PartyA
```

## License

This code follows the structure of the example from https://github.com/corda/cordapp-example licensed under Apache License Version 2.0 (see [Corda License](LICENSE-Corda)).

The CPU-heavy logic is part of Blockbench, licensed under Apache License Version 2.0.
