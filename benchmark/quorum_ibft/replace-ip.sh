#!/bin/bash

if [ $# -lt 1 ]; then
	echo "Usage: $0 <file>"
	exit
fi

FILE=$1
HOSTS=`cat hosts | tr '\n' ' '`
HOSTSA=($HOSTS)

rm -f tmp.json
IDX=0
while read -r LINE; do
	if ! [ -z "`echo $LINE | grep enode`" ]; then
		PREFIX=`echo $LINE | cut -d '@' -f 1`
		SUFFIX=`echo $LINE | cut -d ':' -f 3`
		HOST=${HOSTSA[$IDX]}
		IDX=$(($IDX+1))
		echo "$PREFIX@$HOST:$SUFFIX" >> tmp.json
	else
		echo $LINE >> tmp.json
	fi
done < $FILE
mv tmp.json $FILE
