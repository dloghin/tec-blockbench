#!/bin/bash

FILE=$1
X=`cat $FILE | grep "kvstore duration" | tail -n 200 | cut -d ' ' -f 13 | tr '\n' '+'`
N=200
echo "($X 0)/$N" | bc -l
if [ "$2" == "-v" ]; then
cat $FILE | grep "kvstore duration" | tail -n 200 | cut -d ' ' -f 13
fi
