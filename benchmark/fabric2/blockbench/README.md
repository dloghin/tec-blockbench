# Overview
[block-server.js](block-server.js) and [txn-server.js](txn-server.js) are two driver scripts to work with Blockbench in order to evaluate this Fabric MVP. 
Note that this guide is a complete replacement to this Blockbench [guide](https://github.com/ooibc88/blockbench/tree/master/benchmark/fabric-v2.2).

First start up the network, set up the channel and deploy the chaincode as in this [README](../README.md) and then launch the driver processes. We take the  example in the above README to demonstrate.
```
# Assume under blockbench/
NETWORK_DIR="../test"
ORG_ID=1
CHANNEL_NAME="testchannel"
CC_NAME=fabcar
MODE=open_loop 

node txn-server.js ${NETWORK_DIR} ${ORG_ID} ${CHANNEL_NAME} ${CC_NAME} ${MODE} 8801 > txn-server-8801.log 2>&1 &
node block-server.js ${NETWORK_DIR} ${ORG_ID} ${CHANNEL_NAME} 8800 > block-server.log 2>&1 &
```

# Usage. 
## With curl command on the same machine
```
# Query for the ledger
curl http://localhost:8800/block?num=2
curl http://localhost:8800/height

# Invoke a transaction
curl --header "Content-Type: application/json" \
--request POST \
--data '{"function":"ChangeCarOwner","args":["CAR4", "DUMIT"]}' \
http://localhost:8801/invoke

# Make a query
curl "http://localhost:8801/query?function=queryCar&args=CAR4"
```

## With Blokbench driver
Refer to the blockbench driver [README.md](https://github.com/ooibc88/blockbench/blob/master/src/macro/README.md) for the proper configuration, especially the endpoints.
__NOTE__: Be sure that the channel has been setup and the chaincode (such as YCSB) has been deployed. 

# Shut the helper processes
```
ps aux  |  grep -i block-server  |  awk '{print $2}' | xargs kill -9
ps aux  |  grep -i txn-server  |  awk '{print $2}' | xargs kill -9
```