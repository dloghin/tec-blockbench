#!/bin/bash
#
# Networking benchmarking.
#
# Copyright (C) 2014-2021 Dumitrel Loghin
#

HOST1="192.168.1.1"
HOST2="192.168.1.2"

ssh -o StrictHostKeyChecking=no $HOST1 "killall -9 iperf"
sleep 1
ssh -o StrictHostKeyChecking=no $HOST1 "iperf -s" &
sleep 3
ssh -o StrictHostKeyChecking=no $HOST2 "iperf -c $HOST1 -n 10M"
sleep 3
ssh -o StrictHostKeyChecking=no $HOST2 "ping -c 10 $HOST1"
ssh -o StrictHostKeyChecking=no $HOST1 "killall -9 iperf"