#!/bin/bash

killall -9 xterm
sleep 3
cd java-source/build/nodes
cd PartyA
xterm -e java -Xmx2048m -jar corda.jar &
xterm -e java -jar corda-webserver.jar &
cd ..
cd PartyB
xterm -e java -Xmx2048m -jar corda.jar &
xterm -e java -jar corda-webserver.jar &
cd ..
cd PartyC
xterm -e java -jar corda.jar &
xterm -e java -jar corda-webserver.jar &
cd ..
cd Notary
xterm -e java -jar corda.jar &
