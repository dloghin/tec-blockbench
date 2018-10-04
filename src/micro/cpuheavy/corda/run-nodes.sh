#!/bin/bash

# CMD_PREFIX="xterm -e"
CMD_PREFIX=""

killall -9 xterm
killall -9 java
sleep 3
cd java-source/build/nodes
cd PartyA
$CMD_PREFIX java -Xmx2048m -jar corda.jar &
$CMD_PREFIX java -jar corda-webserver.jar &
cd ..
cd PartyB
$CMD_PREFIX java -Xmx2048m -jar corda.jar &
$CMD_PREFIX java -jar corda-webserver.jar &
cd ..
cd PartyC
$CMD_PREFIX java -jar corda.jar &
$CMD_PREFIX java -jar corda-webserver.jar &
cd ..
cd Notary
$CMD_PREFIX java -jar corda.jar &
