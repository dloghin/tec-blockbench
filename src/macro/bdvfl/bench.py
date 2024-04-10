import os
import itertools
from crypto import *
from fabric import *
from quorum import *

# BACKEND = "fabric"
BACKEND = "quorum"
b = os.environ.get('BENCH_BACKEND')
if b != None:
    BACKEND = b
BENCH_DURATION = 60

GLOBAL_TASK_ID = 2

fabric_global_svr_blk = "http://172.31.5.39:8800"
fabric_global_svr_txn = ["http://172.31.5.39:8802", "http://172.31.0.22:8802", "http://172.31.15.169:8802", "http://172.31.15.40:8802"]

quorum_contract_addr = "0x63c6C14C972849BA9a53848ce018A6E46685F0bd"
quorum_contract_abi = "../../../benchmark/contracts/ethereum/bdvfl.sol.abi"
quorum_global_svr = ["http://172.31.5.39:8000", "http://172.31.0.22:8000", "http://172.31.15.169:8000", "http://172.31.15.40:8000"]

global_counter = 0
global_lock = threading.Lock()

def inc_global_counter():
    global global_counter
    global_lock.acquire()
    global_counter += 1
    global_lock.release()

def build_driver(idx):
    if BACKEND == "fabric":
        return  DriverBDVFLFabric(fabric_global_svr_blk, fabric_global_svr_txn[idx], True)
    elif BACKEND == "quorum":
        return DriverBDVFLEth(quorum_global_svr[idx], quorum_contract_addr, quorum_contract_abi, True)


### Benchmarks

# tid - thread id
def BenchCreateTask(tid, num_ops):
    driver = build_driver(tid % len(fabric_global_svr_txn))

    task = {"taskid": 0, "modelname": "mnist", "modelhash": "11473847766924e13366ce05298cac05039a7ed0339599f5317f808c03ba5f40", "rounds": 10, "nodespks": ["BDDhIgqsR1KtJSl8gqNlo9LYq3FZB1UkHVzZSbFgppAPtUVNAsqPOpxsNjF8yuSdB9vaBjt4vKz/VcGj7VAKr3o=", "BDSFij8/50HwAvnecj/omFXtZegffG5R/YE67esIoXLP/Hz1YaG6V33CBB8Cfh8hrfs+t54Pwa+MrT90/NgV8lk=", "BC0tFlkN3mHJMpOvAJ61xip+Mg8XgFdKN46Lu2zXVivIhu4QXmOZjISZeii9/eoRYkOyzxaBx6F3Z1lUKRACdvs=", "BAPizRIx1QGJd1hbWBsRS8kbjdij3k/AdNXzGA+/MNjoZ0LpqH5OT1m+fWVN7MIQP79R9JdxGQbn/VjdAm7Oc5s="], "aggregatorpk": "BDDhIgqsR1KtJSl8gqNlo9LYq3FZB1UkHVzZSbFgppAPtUVNAsqPOpxsNjF8yuSdB9vaBjt4vKz/VcGj7VAKr3o="}

    start = time.time()
    for i in range(num_ops):
        driver.createTask(task)
        end = time.time()
        if end - start >= BENCH_DURATION:
            print("Client timeout!")
            break

    print("Client BenchCreateTask done.")

def BenchSetPartialParamsHash(tid, num_ops):
    driver = build_driver(tid % len(fabric_global_svr_txn))

    taskId = GLOBAL_TASK_ID
    hash = "11473847766924e13366ce05298cac05039a7ed0339599f5317f808c03ba5f40"
    pvk, _, str_nodePK = loadkey(0)

    start = time.time()
    for i in range(num_ops):
        payload = "{}{}{}{}".format(taskId, str_nodePK, i, hash)
        sig = sign_msg(pvk, payload)
        driver.setPartialParamsHash(taskId, i, str_nodePK, sig, hash)
        end = time.time()
        if end - start >= BENCH_DURATION:
            print("Client timeout!")
            break

    print("Client BenchSetPartialParamsHash done.")

def BenchSetPartialModelHash(tid, num_ops):
    driver = build_driver(tid % len(fabric_global_svr_txn))

    taskId = GLOBAL_TASK_ID
    hash = "11473847766924e13366ce05298cac05039a7ed0339599f5317f808c03ba5f40"
    pvk, _, str_nodePK = loadkey(0)

    start = time.time()
    for i in range(num_ops):
        payload = "{}{}{}".format(taskId, i, hash)
        sig = sign_msg(pvk, payload)
        driver.setPartialModelHash(taskId, i, hash, sig)
        end = time.time()
        if end - start >= BENCH_DURATION:
            print("Client timeout!")
            break

    print("Client BenchSetPartialModelHash done.")

def BenchGetPartialParamsHash(tid, num_ops):
    driver = build_driver(tid % len(fabric_global_svr_txn))

    taskId = GLOBAL_TASK_ID
    _, _, str_nodePK = loadkey(0)

    start = time.time()
    for i in range(num_ops):
        driver.getPartialParamsHash(taskId, i, str_nodePK)
        end = time.time()
        if end - start >= BENCH_DURATION:
            print("Client timeout!")
            break

    print("Client BenchGetPartialParamsHash done.")

def BenchGetInitialModelHash(tid, num_ops):
    driver = build_driver(tid % len(fabric_global_svr_txn))

    taskId = GLOBAL_TASK_ID
    start = time.time()
    for i in range(num_ops):
        driver.getInitialModelHash(taskId)
        end = time.time()
        if end - start >= BENCH_DURATION:
            print("Client timeout!")
            break

    print("Client BenchGetPartialModelHash done.")

def BenchGetPartialModelHash(tid, num_ops):
    driver = build_driver(tid % len(fabric_global_svr_txn))

    taskId = GLOBAL_TASK_ID
    start = time.time()
    for i in range(num_ops):
        driver.getPartialModelHash(taskId, i)
        end = time.time()
        if end - start >= BENCH_DURATION:
            print("Client timeout!")
            break

    print("Client BenchGetPartialModelHash done.")

def BenchMixed(tid, num_ops):
    driver = build_driver(tid % len(fabric_global_svr_txn))

    hash = "11473847766924e13366ce05298cac05039a7ed0339599f5317f808c03ba5f40"
    pvk, _, str_nodePK = loadkey(0)

    start = time.time()

    task = {"taskid": 0, "modelname": "mnist", "modelhash": "11473847766924e13366ce05298cac05039a7ed0339599f5317f808c03ba5f40", "rounds": 10, "nodespks": ["BDDhIgqsR1KtJSl8gqNlo9LYq3FZB1UkHVzZSbFgppAPtUVNAsqPOpxsNjF8yuSdB9vaBjt4vKz/VcGj7VAKr3o=", "BDSFij8/50HwAvnecj/omFXtZegffG5R/YE67esIoXLP/Hz1YaG6V33CBB8Cfh8hrfs+t54Pwa+MrT90/NgV8lk=", "BC0tFlkN3mHJMpOvAJ61xip+Mg8XgFdKN46Lu2zXVivIhu4QXmOZjISZeii9/eoRYkOyzxaBx6F3Z1lUKRACdvs=", "BAPizRIx1QGJd1hbWBsRS8kbjdij3k/AdNXzGA+/MNjoZ0LpqH5OT1m+fWVN7MIQP79R9JdxGQbn/VjdAm7Oc5s="], "aggregatorpk": "BDDhIgqsR1KtJSl8gqNlo9LYq3FZB1UkHVzZSbFgppAPtUVNAsqPOpxsNjF8yuSdB9vaBjt4vKz/VcGj7VAKr3o="}
    driver.createTask(task)
    taskId = tid
    driver.getInitialModelHash(tid)
    inc_global_counter()

    for i in range(num_ops):
        driver.getPartialModelHash(taskId, i)
        inc_global_counter()
        payload = "{}{}{}{}".format(taskId, str_nodePK, i, hash)
        sig = sign_msg(pvk, payload)
        driver.setPartialParamsHash(taskId, i, str_nodePK, sig, hash)
        end = time.time()
        if end - start >= BENCH_DURATION:
            print("Client timeout!")
            break

    print("Client BenchMixed done.")

def BlockPoll(duration, target_tx):
    driver = build_driver(0)

    start = time.time()
    end = start
    start_tip = int(driver.getBlockchainHeight())
    total_tx = 0
    print("Start BlockPoll for {} s".format(duration))
    while (end - start < duration):
        curr_tip = int(driver.getBlockchainHeight())
        print("Start tip {} Current tip {} Current txns {}".format(start_tip, curr_tip, total_tx))
        while start_tip == curr_tip:
            time.sleep(0.25)
            end = time.time()
            if end - start >= duration:
                break
            curr_tip = int(driver.getBlockchainHeight())
        while start_tip < curr_tip:
            total_tx += driver.getBlockTxnsLen(start_tip)
            start_tip += 1
        end = time.time()
        if total_tx >= target_tx:
            break
    print("Duration: {} s; Requests: {}; Throughput: {} tps".format(end-start, total_tx, float(total_tx)/float(end-start)))

def BenchSet(nthreads, fname = ""):
    global global_counter
    
    func = BenchCreateTask
    if fname == "CreateTask":
        func = BenchCreateTask
    elif fname == "SetPartialParamsHash":
        func = BenchSetPartialParamsHash
    elif fname == "SetPartialModelHash":
        func = BenchSetPartialModelHash
    elif fname == "Mixed":
        global_counter = 0
        func = BenchMixed
    else:
        fname = "CreateTask"

    print("Running BenchSet for blockchain {} with function: {} and thread count: {}".format(BACKEND, fname, nthreads))

    t0 = threading.Thread(target=BlockPoll, args=(BENCH_DURATION, 1000 * nthreads,))
    t0.start()
    ths = []
    for i in range(nthreads):
        th = threading.Thread(target=func, args=(i, 1000,))
        ths.append(th)
        th.start()

    for i in range(nthreads):
        ths[i].join()
    t0.join()

    if fname == "Mixed":
        print("Total Get ops: {}".format(global_counter))

    print("Bench Set Done!")

def BenchGet(nthreads, fname = ""):
    ths = []
    start = time.time()
    func = BenchGetInitialModelHash
    if fname == "GetInitialModelHash":
        func = BenchGetInitialModelHash
    elif fname == "GetPartialModelHash":
        func = BenchGetPartialModelHash
    elif fname == "GetPartialParamsHash":
        func = BenchGetPartialParamsHash
    else:
        fname = "GetInitialModelHash"

    print("Running BenchGet for blockchain {} with function: {} and thread count: {}".format(BACKEND, fname, nthreads))

    for i in range(nthreads):
        th = threading.Thread(target=func, args=(i, 1000,))
        ths.append(th)
        th.start()

    for i in range(nthreads):
        ths[i].join()
    end = time.time()
    total_tx = 1000 * nthreads
    print("Duration: {} s; Requests: {}; Throughput: {} tps".format(end-start, total_tx, float(total_tx)/float(end-start)))
    print("Bench Get Done!")

### Main
if __name__ == '__main__':
    if len(sys.argv) > 1:
        if sys.argv[1] == "bench_get":
            if len(sys.argv) == 2:
                BenchGet(8)
            else:
                BenchGet(int(sys.argv[3]), sys.argv[2])
        elif sys.argv[1] == "bench_set":
            if len(sys.argv) == 2:
                BenchSet(8)
            else:
                BenchSet(int(sys.argv[3]), sys.argv[2])