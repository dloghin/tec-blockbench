# Micro Benchmark Workload - IOHeavy in Corda
## Usage

To build the source and run 3 nodes and one notary, run:
```
$ ./build-run.sh
```
To issue some transactions, run:
```
$ ./run-test.sh
```
To write and scan 1000 values in PartyB (port 10012) with counter-party PartyA, run:
```
$ curl -X PUT localhost:10012/api/ioheavy/ioheavy?command=1\&startKey=1\&numKeys=1000\&partyName=PartyA
$ curl -X PUT localhost:10012/api/ioheavy/ioheavy?command=2\&startKey=1\&numKeys=1000\&partyName=PartyA
```
Write command has code 1, scan command has code 2.
 
## License

This code follows the structure of the example from https://github.com/corda/cordapp-example licensed under Apache License Version 2.0 (see [Corda License](LICENSE-Corda)).

The CPU-heavy logic is part of Blockbench, licensed under Apache License Version 2.0.
