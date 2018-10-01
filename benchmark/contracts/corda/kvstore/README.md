# Micro Benchmark Workload - KVStore in Corda
## Usage

To build the source and run 3 nodes and one notary, run:
```
$ ./build-run.sh
```
To issue some transactions, run:
```
$ ./run-test.sh
```

API format:
```
curl -X PUT localhost:10012/api/kvstore/kvstore?command=<cmd>\&key=<key>\&val=<val>\&partyName=<party>
```
where command can be:
 * 1 - write
 * 2 - delete
 * 3 - update
 * 4 - read

partyName and key are mandatory, val is mandatory only for write and update. Read returns the state in JSON format.
 
## License

This code follows the structure of the example from https://github.com/corda/cordapp-example licensed under Apache License Version 2.0 (see [Corda License](LICENSE-Corda)).

The KVStore logic is part of Blockbench, licensed under Apache License Version 2.0.
