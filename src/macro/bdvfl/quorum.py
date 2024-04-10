import json
import sys
import time
import pprint

from web3.providers.eth_tester import EthereumTesterProvider
from eth_tester import PyEVMBackend
from web3 import Web3
from solcx import compile_source

quorum_global_svr = "http://172.31.5.39:8000"
quorum_global_contract_address = "0x63c6C14C972849BA9a53848ce018A6E46685F0bd"
quorum_global_contract_path = "../../../benchmark/contracts/ethereum/bdvfl.sol"

def deploy_bdvfl_contract():
    contract_name = "bdvfl.sol"
    contract_path = quorum_global_contract_path
    abi = []
    with open(contract_path + ".abi", mode='rt') as tfile:
        abi = json.loads(tfile.read())
    bcode = ""
    with open(contract_path + ".bin", mode='rt') as tfile:
        bcode = tfile.read()

    w3 = Web3(Web3.HTTPProvider(quorum_global_svr))
    print("Connected to Geth: {}".format(w3.isConnected()))

    w3.eth.defaultAccount = w3.eth.accounts[0]
    unlocked = w3.geth.personal.unlockAccount(w3.eth.defaultAccount,'',60000)
    print("Account unlocked: {}".format(unlocked))

    tx = w3.eth.contract(abi=abi,bytecode=bcode).constructor().buildTransaction({'gasPrice': 0})
    txh = w3.geth.personal.sendTransaction(tx,'')
    rcpt = w3.eth.waitForTransactionReceipt(txh, 30, 0.2)
    address = rcpt['contractAddress']
    print("Contract {} deployed at {}".format(contract_name, address))
    return address

class DriverKVEth:
    def __init__(self, svr_addr, contract_addr, abi_file):
        abi = []
        with open(abi_file, mode='rt') as tfile:
            abi = json.loads(tfile.read())
        self.w3 = Web3(Web3.HTTPProvider(svr_addr))
        print("Connected to Geth: {}".format(self.w3.isConnected()))
        self.w3.eth.defaultAccount = self.w3.eth.accounts[0]
        unlocked = self.w3.geth.personal.unlockAccount(self.w3.eth.defaultAccount,'',60000)
        print("Account unlocked: {}".format(unlocked))
        self.contract = self.w3.eth.contract(address=contract_addr, abi=abi)
        self.address = contract_addr

    def writeKV(self, key, val):
        tx = self.contract.functions.set(key, val).buildTransaction({'from':self.w3.eth.defaultAccount, 'gasPrice': 0})
        txh = self.w3.geth.personal.sendTransaction(tx,'')
        rcpt = self.w3.eth.waitForTransactionReceipt(txh, 30, 0.2)
        print(rcpt)
        # print(rcpt["status"])

    def readKV(self, key):
        print(self.contract.functions.get(key).call())

    def setVar(self):
        tx = self.contract.functions.setVar(21).buildTransaction({'from': self.w3.eth.defaultAccount, 'gasPrice': 0})
        txh = self.w3.geth.personal.sendTransaction(tx,'')
        rcpt = self.w3.eth.waitForTransactionReceipt(txh, 30, 0.2)
        print(rcpt)
        # print(rcpt["status"])

    def getVar(self):
        print(self.contract.functions.getVar().call())

class DriverBDVFLEth:
    def __init__(self, svr_addr, contract_addr, abi_file, quiet = False):
        self.quiet = quiet
        abi = []
        with open(abi_file, mode='rt') as tfile:
            abi = json.loads(tfile.read())
        self.w3 = Web3(Web3.HTTPProvider(svr_addr))
        print("Connected to Geth: {}".format(self.w3.isConnected()))
        self.w3.eth.defaultAccount = self.w3.eth.accounts[0]
        unlocked = self.w3.geth.personal.unlockAccount(self.w3.eth.defaultAccount,'',60000)
        print("Account unlocked: {}".format(unlocked))
        self.contract = self.w3.eth.contract(address=contract_addr, abi=abi)
        self.address = contract_addr

    def getBlockchainHeight(self):
        return self.w3.eth.blockNumber

    def getBlockTxns(self, blkid):
        return []

    def getBlockTxnsLen(self, blkid):
        return self.w3.eth.getBlockTransactionCount(blkid)

    def _send_tx(self, func):
        tx = func.buildTransaction({'from': self.w3.eth.defaultAccount, 'gas': 1000000, 'gasPrice': 0})
        txh = self.w3.geth.personal.sendTransaction(tx,'')
        if not self.quiet:
            print(tx)
        if not self.quiet:
            print(self.w3.eth.waitForTransactionReceipt(txh, 30, 0.2))

    def createTask(self, task):
        func = self.contract.functions.CreateBDVFLTask(task["modelname"], task["modelhash"], task["rounds"], self.w3.eth.defaultAccount, [self.w3.eth.defaultAccount])
        self._send_tx(func)

    def getInitialModelHash(self, taskId):
        return self.contract.functions.GetInitialModelHash(taskId).call()

    def setPartialParamsHash(self, taskId, round, str_nodePK, str_nodeSig, str_params):
        func = self.contract.functions.SetPartialParamsHash(taskId, round, str_params)
        self._send_tx(func)

    def getPartialParamsHash(self, taskId, round, str_nodePK = ""):
        return self.contract.functions.GetPartialParamsHash(taskId, round, self.w3.eth.defaultAccount).call()

    def setPartialModelHash(self, taskId, round, hash, str_nodeSig = ""):
        func = self.contract.functions.SetPartialModelHash(taskId, round, hash)
        self._send_tx(func)

    def getPartialModelHash(self, taskId, round):
        return self.contract.functions.GetPartialModelHash(taskId, round).call()

### Tests
def test_bdvfl():
    driver = DriverBDVFLEth(quorum_global_svr, quorum_global_contract_address, quorum_global_contract_path + ".abi")
    task = {"taskid": 0, "modelname": "mnist", "modelhash": "0xffff", "rounds": 10}
    driver.createTask(task)
    print(driver.getInitialModelHash(0))
    driver.setPartialParamsHash(1, 0, "", "", "0xfabc")
    print(driver.getPartialParamsHash(1, 0, ""))
    driver.setPartialModelHash(1, 0, "0xcccc")
    print(driver.getPartialModelHash(1, 0))
    print(driver.getBlockchainHeight())

### Main
if __name__ == '__main__':
    if len(sys.argv) > 1:
        if sys.argv[1] == "deploy":
            deploy_bdvfl_contract()
    else:
        test_bdvfl()
