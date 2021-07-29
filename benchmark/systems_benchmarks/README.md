# System Benchmarks

This README contains information on how to run the benchmarks presented in Section IV.B of our paper.

## CPU

### Coremark

Get CoreMark version 1.01 [from Github](https://github.com/eembc/coremark).

Compilation flags:

 - On Xeon

```
export PORT_DIR="linux64"
export XCFLAGS="-Wall -O3 -m64 -march=native -ffast-math -funroll-loops"
```

 - On Graviton

```
export PORT_DIR="linux64"
export XCFLAGS="-Wall -O3 -ftree-vectorize -ffast-math -faggressive-loop-optimizations"
```

 - On Jetson TX2

```
export PORT_DIR="linux64"
export XCFLAGS="-Wall -O3 -mcpu=cortex-a57 -mtune=cortex-a57 -ftree-vectorize -ffast-math -faggressive-loop-optimizations"
```

 - On Raspberry Pi 4

```
export PORT_DIR="linux64"
export XCFLAGS="-Wall -O3 -mcpu=cortex-a72 -mtune=cortex-a72 -ftree-vectorize -ffast-math -faggressive-loop-optimizations"
```

Make sure ``coremark.exe`` is in the ``cpu`` folder such that ``EXEC`` variable in [cpu/run-coremark.sh](cpu/run-coremark.sh) points to it. Then, run the benchmark on a specified number of cores (e.g., 4 cores):

```
$ cd cpu
$ ./run-coremark.sh 4
```

The performance is indicated by the ``Iterations/Sec`` field.

### Secp256r1

Compile and run:

```
$ cd secp256r1
$ go build
$ ./secp256r1
Usage: ./secp256r1 <sign|verify> <size>
$ ./secp256r1 sign 64
...
$ ./secp256r1 verify 64
```

To get the IPC, run with ``perf stat``:

```
$ perf stat ./secp256r1 sign 64
Message size 64
Iterations 100000
sign time 2.50057901s

 Performance counter stats for './secp256r1 sign 64':

          2,659.61 msec task-clock                #    1.058 CPUs utilized          
             7,777      context-switches          #    0.003 M/sec                  
               220      cpu-migrations            #    0.083 K/sec                  
             2,245      page-faults               #    0.844 K/sec                  
     9,907,521,194      cycles                    #    3.725 GHz                      (82.68%)
        55,104,480      stalled-cycles-frontend   #    0.56% frontend cycles idle     (82.87%)
     6,679,850,705      stalled-cycles-backend    #   67.42% backend cycles idle      (83.49%)
    25,212,445,358      instructions              #    2.54  insn per cycle         
                                                  #    0.26  stalled cycles per insn  (83.54%)
       927,473,905      branches                  #  348.725 M/sec                    (83.74%)
         4,396,903      branch-misses             #    0.47% of all branches          (83.70%)

       2.512652296 seconds time elapsed

       2.453044000 seconds user
       0.216325000 seconds sys
```

### Secp256k1

Compile and run:

```
$ cd secp256k1
$ go build
$ ./secp256k1 1024
```

To get the IPC, run with ``perf stat``:

```
$ perf stat ./secp256k1 1024
Message size 1024
Iterations 100000000
sign time 303.013216ms

 Performance counter stats for './secp256k1 1024':

            318.29 msec task-clock                #    1.002 CPUs utilized          
               128      context-switches          #    0.402 K/sec                  
                 4      cpu-migrations            #    0.013 K/sec                  
             1,633      page-faults               #    0.005 M/sec                  
     1,345,289,206      cycles                    #    4.227 GHz                      (82.46%)
         1,148,013      stalled-cycles-frontend   #    0.09% frontend cycles idle     (83.67%)
       721,695,431      stalled-cycles-backend    #   53.65% backend cycles idle      (83.67%)
     4,067,908,212      instructions              #    3.02  insn per cycle         
                                                  #    0.18  stalled cycles per insn  (83.66%)
       498,205,827      branches                  # 1565.262 M/sec                    (83.67%)
            20,553      branch-misses             #    0.00% of all branches          (82.88%)

       0.317529599 seconds time elapsed

       0.314839000 seconds user
       0.003985000 seconds sys
```

## Memory

Get ``lmbench-3.0-a9`` [from sourceforge](https://sourceforge.net/projects/lmbench/files/latest/download).

```
...lmbench/src$ cat version.h 
#define	MAJOR	3
#define	MINOR	-9	/* negative is alpha, it "increases" */
```

Compile ``lmbench``. The binaries are in ``bin``.

```
$ cd lmbench/src
$ make
$ ls ../bin
```

Run ``lmbench`` using our script. Make sure variable ``EXEC`` in [memory/run-lmbench.sh](memory/run-lmbench.sh) points to the ``lmbench`` ``bw_mem`` binary.

```
$ cd memory
$ ./run-lmbench.sh
```

The results are in a CSV file with the suffix ``-rdwr.csv``.

## Storage

Make sure you have ``dd`` and ``ioping`` installed. To install ``ioping``, run:

```
$ sudo apt install ioping
```

Then, run the benchmark using sudo:

```
$ cd storage
$ sudo ./storage-bench.sh
```

## Networking

This benchmark needs two nodes. Set ``HOST1`` and ``HOST2`` in [networking/networking-bench.sh](networking/networking-bench.sh) to their IP. Make sure ``ssh`` is working without password between these two nodes.

Makesure you have ``ping`` and ``iperf`` installed on each node.

```
$ sudo apt update
$ sudo apt install iputils-ping
$ sudo apt install iperf
```

Run the benchmark:

```
$ cd networking
$ ./networking-bench.sh
```
