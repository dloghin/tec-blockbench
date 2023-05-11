#!/bin/bash

FILE=$1

cat $FILE | grep "EVM call took" | tail -n 200 | cut -d '=' -f 2 | tr 'µ' 'x' | cut -d 'x' -f 1
