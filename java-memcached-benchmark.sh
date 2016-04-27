#!/bin/bash
set -x

#taskset -c 4-23 \
java -jar dist/java-memcached-benchmark.jar -h 127.0.0.1 -p 11211 -t 100 -r 10000 -b 15 -c 100 -op get -opTimeout 5000
