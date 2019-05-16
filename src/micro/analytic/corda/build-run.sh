#!/bin/bash

killall -9 xterm
sleep 3
./gradlew clean
./gradlew deployNodes
cd java-source/build/nodes
java -jar runnodes.jar
