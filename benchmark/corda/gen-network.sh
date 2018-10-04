#!/bin/bash

HOSTS_FILE="hosts.txt"

if ! [ -f "$HOSTS_FILE" ]; then
	echo "Please add a host file first!"
	exit 1
fi

NET_DIR="network"
rm -rf $NET_DIR
mkdir $NET_DIR

ID=0
while read HOST; do
	ID=$(($ID+1))
	echo "Generating node $ID ($HOST)"
	NODE_FILE="$NET_DIR/party$ID""_node.conf"
	echo "myLegalName=\"O=Party$ID,L=Sg,C=SG\"" > $NODE_FILE
	echo "p2pAddress=\"$HOST:10007\"" >> $NODE_FILE
	echo "rpcSettings {" >> $NODE_FILE
	echo -e "\taddress=\"$HOST:10008\"" >> $NODE_FILE
	echo -e "\tadminAddress=\"$HOST:10048\"" >> $NODE_FILE
	echo "}" >> $NODE_FILE
	echo "rpcUsers=[" >> $NODE_FILE
    	echo "{" >> $NODE_FILE
	echo -e "\tpassword=test" >> $NODE_FILE
        echo -e "\tpermissions=[" >> $NODE_FILE
        echo -e "\t\tALL" >> $NODE_FILE
        echo -e "\t]" >> $NODE_FILE
        echo -e "\tuser=user1" >> $NODE_FILE
    	echo "}" >> $NODE_FILE
	echo "]" >> $NODE_FILE
	echo "webAddress=\"$HOST:10009\"" >> $NODE_FILE
done < $HOSTS_FILE

cp notary_node.conf $NET_DIR/
cp ../contracts/corda/kvstore/java-source/build/libs/cordapp-example-0.1.jar $NET_DIR/

java -jar corda-network-bootstrapper-3.2-corda-executable.jar --dir $NET_DIR 
