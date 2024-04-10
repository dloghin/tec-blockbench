import sys
import time
import requests
import threading
from crypto import *

fabric_global_svr_blk = "http://10.0.0.1:8800"
fabric_global_svr_txn = "http://10.0.0.1:8802"

class DriverKVFabric:
    def __init__(self, blkAddr, txnAddr):
        self.blkServerAddr = blkAddr
        self.txnServerAddr = txnAddr

    def getBlockchainHeight(self):
        url = self.blkServerAddr + "/height"
        response = requests.get(url)
        rj = response.json()
        return rj["height"]

    def writeKV(self, key, val):
        url = self.txnServerAddr + "/invoke"
        req = {"function": "Write", "args": ["{}".format(key), "{}".format(val)]}
        response = requests.post(url, json=req)
        print("Write Request Response: ")
        print(response.json())

    def readKV(self, key):
        url = self.txnServerAddr + "/query"
        req = {"function": "Read", "args": ["{}".format(key)]}
        response = requests.get(url, json=req)
        print("Read Request Response: ")
        print(response.json())

class DriverBDVFLFabric:
    def __init__(self, blkAddr, txnAddr, quiet = False):
        self.blkServerAddr = blkAddr
        self.txnServerAddr = txnAddr
        self.quiet = quiet

    def getBlockchainHeight(self):
        url = self.blkServerAddr + "/height"
        response = requests.get(url)
        rj = response.json()
        return rj["height"]

    def getBlockTxns(self, blkid):
        url = self.blkServerAddr + "/block/?num={}".format(blkid)
        response = requests.get(url)
        rj = response.json()
        if rj["status"] == "0":
            return rj["txns"]
        return []

    def getBlockTxnsLen(self, blkid):
        url = self.blkServerAddr + "/block/?num={}".format(blkid)
        response = requests.get(url)
        rj = response.json()
        if rj["status"] == "0":
            return len(rj["txns"])
        return 0

    def createTask(self, task):
        url = self.txnServerAddr + "/invoke"
        s = "{}".format(task).replace("'",'"')
        req = {"function": "CreateBDVFLTask", "args": [s]}
        response = requests.post(url, json=req)
        if not self.quiet:
            print("CreateBDVFLTask Request Response: ")
            print(response.json())

    def getInitialModelHash(self, taskId):
        url = self.txnServerAddr + "/query"
        req = {"function": "GetInitialModelHash", "args": ["{}".format(taskId)]}
        response = requests.get(url, json=req)
        if not self.quiet:
            print("GetModelHash Request Response: ")
            print(response.json())
        return response.json()["result"]

    def setPartialParamsHash(self, taskId, round, str_nodePK, str_nodeSig, str_params):
        url = self.txnServerAddr + "/invoke"
        args = ["{}".format(taskId), str_nodePK, str_nodeSig, round, str_params]
        req = {"function": "SetPartialParamsHash", "args": args}
        response = requests.post(url, json=req)
        if not self.quiet:
            print("SetPartialParams Request Response: ")
            print(response.json())

    def getPartialParamsHash(self, taskId, round, str_nodePK):
        url = self.txnServerAddr + "/query"
        req = {"function": "GetPartialParamsHash", "args": ["{}".format(taskId), str_nodePK, round]}
        response = requests.get(url, json=req)
        if not self.quiet:
            print("GetPartialParams Request Response: ")
            print(response.json())
        return response.json()["result"]

    def setPartialModelHash(self, taskId, round, hash, str_nodeSig):
        url = self.txnServerAddr + "/invoke"
        args = ["{}".format(taskId), round, hash, str_nodeSig]
        req = {"function": "SetPartialModelHash", "args": args}
        response = requests.post(url, json=req)
        if not self.quiet:
            print("SetPartialModelHash Request Response: ")
            print(response.json())

    def getPartialModelHash(self, taskId, round):
        url = self.txnServerAddr + "/query"
        req = {"function": "GetPartialModelHash", "args": ["{}".format(taskId), round]}
        response = requests.get(url, json=req)
        if not self.quiet:
            print("GetPartialModelHash Request Response: ")
            print(response.json())
        return response.json()["result"]

### Tests
def testKV():
    driver = DriverKVFabric(fabric_global_svr_blk, fabric_global_svr_txn)
    print("Blockchain height: {}".format(driver.getBlockchainHeight()))
    driver.writeKV("key9", "val9")
    driver.readKV("key123")

def testBDVFL():
    driver = DriverBDVFLFabric(fabric_global_svr_blk, fabric_global_svr_txn)
    bh = int(driver.getBlockchainHeight())
    print("Blockchain height: {}".format(bh))
    driver.getBlockTxns(bh - 1)
    task = {"taskid": 0, "modelname": "mnist", "modelhash": "0x1234", "rounds": 10, "nodespks": ["node1", "node2", "node3"], "aggregatorpk": "node0"}
    driver.createTask(task)

### Main
if __name__ == '__main__':
    testBDVFL()
