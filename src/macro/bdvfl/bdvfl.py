import copy
import pickle
import shutil
import time
import torch
from torchvision.datasets import MNIST
from torch.utils.data import random_split, DataLoader
import torchvision.transforms as transforms
import sys
import os
from crypto import *
from fabric import *
from quorum import *

# Global Params
train_dataset = MNIST('data', train=True, download=True, transform=transforms.ToTensor())
test_dataset = MNIST('data', train=False, download=True, transform=transforms.ToTensor())
train_dataset, dev_dataset = random_split(train_dataset, [int(len(train_dataset) * 0.83), int(len(train_dataset) * 0.17)])

total_train_size = len(train_dataset)
total_test_size = len(test_dataset)
total_dev_size = len(dev_dataset)

classes = 10
input_dim = 784

num_clients = 4
rounds = 10
batch_size = 128
epochs_per_client = 3
learning_rate = 2e-2

total_train_size, total_dev_size, total_test_size

BACKEND = "fabric"

fabric_global_svr_blk = "http://10.0.0.1:8800"
fabric_global_svr_txn = "http://10.0.0.1:8802"

quorum_contract_addr = "0x8F42A361b666e6595bda58CCF4E12eCD55D8FF53"
quorum_contract_abi = "../../../benchmark/contracts/ethereum/bdvfl.sol.abi"
quorum_global_svr = "http://10.0.0.2:8000"

def build_driver():
    if BACKEND == "fabric":
        return  DriverBDVFLFabric(fabric_global_svr_blk, fabric_global_svr_txn)
    elif BACKEND == "quorum":
        return DriverBDVFLEth(quorum_global_svr, quorum_contract_addr, quorum_contract_abi)

def get_device():
    return torch.device('cuda') if torch.cuda.is_available() else torch.device('cpu')

def to_device(data, device):
    if isinstance(data, (list, tuple)):
        return [to_device(x, device) for x in data]
    return data.to(device, non_blocking=True)

class DeviceDataLoader(DataLoader):
        def __init__(self, dl, device):
            self.dl = dl
            self.device = device

        def __iter__(self):
            for batch in self.dl:
                yield to_device(batch, self.device)

        def __len__(self):
            return len(self.dl)

class FederatedNet(torch.nn.Module):
    def __init__(self):
        super().__init__()
        self.conv1 = torch.nn.Conv2d(1, 20, 7)
        self.conv2 = torch.nn.Conv2d(20, 40, 7)
        self.maxpool = torch.nn.MaxPool2d(2, 2)
        self.flatten = torch.nn.Flatten()
        self.linear = torch.nn.Linear(2560, 10)
        self.non_linearity = torch.nn.functional.relu
        self.track_layers = {'conv1': self.conv1, 'conv2': self.conv2, 'linear': self.linear}

    def forward(self, x_batch):
        out = self.conv1(x_batch)
        out = self.non_linearity(out)
        out = self.conv2(out)
        out = self.non_linearity(out)
        out = self.maxpool(out)
        out = self.flatten(out)
        out = self.linear(out)
        return out

    def get_track_layers(self):
        return self.track_layers

    def apply_parameters(self, parameters_dict):
        with torch.no_grad():
            for layer_name in parameters_dict:
                self.track_layers[layer_name].weight.data *= 0
                self.track_layers[layer_name].bias.data *= 0
                self.track_layers[layer_name].weight.data += parameters_dict[layer_name]['weight']
                self.track_layers[layer_name].bias.data += parameters_dict[layer_name]['bias']

    def get_parameters(self):
        parameters_dict = dict()
        for layer_name in self.track_layers:
            parameters_dict[layer_name] = {
                'weight': self.track_layers[layer_name].weight.data,
                'bias': self.track_layers[layer_name].bias.data
            }
        return parameters_dict

    def batch_accuracy(self, outputs, labels):
        with torch.no_grad():
            _, predictions = torch.max(outputs, dim=1)
            return torch.tensor(torch.sum(predictions == labels).item() / len(predictions))

    def _process_batch(self, batch):
        images, labels = batch
        outputs = self(images)
        loss = torch.nn.functional.cross_entropy(outputs, labels)
        accuracy = self.batch_accuracy(outputs, labels)
        return (loss, accuracy)

    def fit(self, dataset, epochs, lr, device, batch_size=128, opt=torch.optim.SGD):
        dataloader = DeviceDataLoader(DataLoader(dataset, batch_size, shuffle=True), device)
        optimizer = opt(self.parameters(), lr)
        history = []
        for epoch in range(epochs):
            losses = []
            accs = []
            for batch in dataloader:
                loss, acc = self._process_batch(batch)
                loss.backward()
                optimizer.step()
                optimizer.zero_grad()
                loss.detach()
                losses.append(loss)
                accs.append(acc)
            avg_loss = torch.stack(losses).mean().item()
            avg_acc = torch.stack(accs).mean().item()
            history.append((avg_loss, avg_acc))
        return history

    def evaluate(self, dataset, device, batch_size=128):
        dataloader = DeviceDataLoader(DataLoader(dataset, batch_size), device)
        losses = []
        accs = []
        with torch.no_grad():
            for batch in dataloader:
                loss, acc = self._process_batch(batch)
                losses.append(loss)
                accs.append(acc)
        avg_loss = torch.stack(losses).mean().item()
        avg_acc = torch.stack(accs).mean().item()
        return (avg_loss, avg_acc)

class Client:
    def __init__(self, client_idx, client_id, dataset, device):
        self.client_index = client_idx
        self.client_id = client_id
        self.dataset = dataset
        self.device = device

    def get_dataset_size(self):
        return len(self.dataset)

    def get_client_id(self):
        return self.client_id

    def get_client_index(self):
        return self.client_index

    def train(self, parameters_dict):
        net = to_device(FederatedNet(), self.device)
        net.apply_parameters(parameters_dict)
        train_history = net.fit(self.dataset, epochs_per_client, learning_rate, self.device, batch_size)
        print('{}: Loss = {}, Accuracy = {}'.format(self.client_id, round(train_history[-1][0], 4), round(train_history[-1][1], 4)))
        return net.get_parameters()

def getModelHashFromLocalCopy():
    with open('model.pt', mode='rb') as bfile:
        contents = bfile.read()
        return get_hash(contents)
    return ""

def compute_cks(pvkeys, pbkeys):
    cks = {}
    for i in range(num_clients):
        for j in range(num_clients):
            if i == j:
                cks["{}_{}".format(i, j)] = 1
            else:
                spk = bytes(pbkeys[j], 'utf-8')
                data = pvkeys[i].ecdsa_serialize_compact(pvkeys[i].ecdsa_sign(spk))
                cks["{}_{}".format(i, j)] = get_hash(data)
    return cks

def compute_rb(client_idx, b, t, nclients, cks):
    i = client_idx
    rb = 0.0
    for j in range(nclients):
        payload = bytes("{}{}{}".format(cks["{}_{}".format(i, j)], b, t), 'utf-8')
        rbj = get_hash_digest(payload) >> 256
        if i < j:
            rb += rbj
        elif i > j:
            rb -= rbj
    return rb

def save_partial_params(client_idx, task_id, round, params):
    filename = "task_{}/params_{}_{}.bin".format(task_id, client_idx, round)
    with open(filename, mode='wb') as bfile:
        bfile.write(params)
        bfile.close()

def save_current_params(task_id, round, params):
    filename = "task_{}/global_params_{}.pickle".format(task_id, round)
    with open(filename, 'wb') as handle:
        pickle.dump(params, handle, protocol=pickle.HIGHEST_PROTOCOL)
    with open(filename, 'rb') as handle:
        data = handle.read()
        return get_hash(data)

def load_current_params(task_id, round):
    filename = "task_{}/global_params_{}.pickle".format(task_id, round)
    params = {}
    with open(filename, 'rb') as handle:
        params = pickle.load(handle)
    with open(filename, 'rb') as handle:
        data = handle.read()
        return params, get_hash(data)

def compute_blinding_factors(client_idx, nclients, cks, client_parameters, fraction, round):
    rbs = dict([(layer_name, {'weight': 0, 'bias': 0}) for layer_name in client_parameters])
    for layer_name in client_parameters:
        rbs[layer_name]['weight'] = compute_rb(client_idx, client_parameters[layer_name]['weight'], round, nclients, cks)
        rbs[layer_name]['bias'] = compute_rb(client_idx, client_parameters[layer_name]['bias'], round, nclients, cks)
    return rbs

def compute_blinded_params(client_idx, nclients, cks, client_parameters, fraction, round):
    rbs = compute_blinding_factors(client_idx, nclients, cks, client_parameters, fraction, round)
    for layer_name in client_parameters:
        client_parameters[layer_name]['weight'] = fraction * client_parameters[layer_name]['weight'] + rbs[layer_name]['weight']
        client_parameters[layer_name]['bias'] = fraction * client_parameters[layer_name]['bias'] + rbs[layer_name]['bias']
    return client_parameters, rbs

# Initialize FL Task and upload it to the blockchain
def init():
    # create model, save it, and compute its hash
    net = FederatedNet()
    torch.save(net.state_dict(), 'model.pt')
    h = getModelHashFromLocalCopy()

    # load public keys
    _, _, str_pbkeys = loadkeys()
    jsonkeys = []
    for k in str_pbkeys:
        jsonkeys.append(str_pbkeys[k])

    # create task on blockchain
    driver = build_driver()
    task = {"taskid": 0, "modelname": "mnist", "modelhash": "{}".format(h), "rounds": rounds, "nodespks": jsonkeys, "aggregatorpk": str_pbkeys[0]}
    driver.createTask(task)

# Run FL Task
def runfl():
    taskId = 2

    # select GPU if available
    device = get_device()

    # create temp dir
    dirname = "task_{}".format(taskId)
    if os.path.exists(dirname):
        if os.path.isdir(dirname):
            shutil.rmtree(dirname)
        else:
            os.remove(dirname)
    os.makedirs(dirname)

    examples_per_client = total_train_size // num_clients
    client_datasets = random_split(train_dataset, [min(i + examples_per_client,
        total_train_size) - i for i in range(0, total_train_size, examples_per_client)])
    clients = [Client(i, 'client_' + str(i), client_datasets[i], device) for i in range(num_clients)]

    pvkeys, pbkeys, str_pbkeys = loadkeys()
    cks = compute_cks(pvkeys, str_pbkeys)

    # blockchain driver
    driver = build_driver()

    # Load initial model and check its hash
    hl = ""
    with open('model.pt', mode='rb') as bfile:
        contents = bfile.read()
        hl = get_hash(contents)
        print("Hash: " + hl)

    hb = driver.getInitialModelHash(taskId)
    if hl != hb:
        print("Model hash different from blockchain version!")
        return

    m_state_dict = torch.load('model.pt')
    net = FederatedNet()
    net.load_state_dict(m_state_dict)
    global_net = to_device(net, device)
    h = save_current_params(taskId, 0, global_net.get_parameters())
    payload = "{}{}{}".format(taskId, 0, h)
    sig = sign_msg(pvkeys[0], payload)
    driver.setPartialModelHash(taskId, 0, h, sig)
    time.sleep(3)

    # Run rounds
    for i in range(rounds):
        print('Start Round {} ...'.format(i + 1))

        # Load params and check hash vs. blockchain
        curr_parameters, hl = load_current_params(taskId, i)
        hb = driver.getPartialModelHash(taskId, i)
        print("Hash local: {}".format(hl))
        print("Hash chain: {}".format(hb))
        if hl != hb:
            print("Model hash different from blockchain version!")
            return
        new_parameters = dict([(layer_name, {'weight': 0, 'bias': 0}) for layer_name in curr_parameters])

        all_client_parameters = {}
        all_blinding_factors = {}
        for client in clients:
            # Run local training (as in Algorithm 1 [1])
            client_parameters = client.train(curr_parameters)
            fraction = client.get_dataset_size() / total_train_size

            # Blind params
            client_parameters, blinding_factors = compute_blinded_params(client.get_client_index(), len(clients), cks, client_parameters, fraction, i)
            all_client_parameters[client.get_client_index()] = client_parameters
            all_blinding_factors[client.get_client_index()] = blinding_factors

            # Upload to blockchain and save params
            cid = client.get_client_index()
            cpk = str_pbkeys[cid]
            pvk = pvkeys[cid]
            contents = pickle.dumps(client_parameters)
            print("Parameter size: {}".format(len(contents)))
            # str_params = base64.b64encode().decode("ascii")
            h_params = get_hash(contents)
            payload = "{}".format(taskId) + cpk + "{}".format(i) + h_params
            print("Payload: {}".format(payload))
            str_sig = sign_msg(pvk, payload)
            driver.setPartialParamsHash(taskId, i, cpk, str_sig, h_params)
            save_partial_params(cid, taskId, i, contents)

        # Aggregation (as in Algorithm 2 [1])
        print("Start aggregation")
        for client in clients:
            client_parameters = all_client_parameters[client.get_client_index()]
            for layer_name in client_parameters:
                new_parameters[layer_name]['weight'] += client_parameters[layer_name]['weight']
                new_parameters[layer_name]['bias'] += client_parameters[layer_name]['bias']
        global_net.apply_parameters(new_parameters)
        train_loss, train_acc = global_net.evaluate(train_dataset, device)
        print("Aggregation model loss {} and acc {}".format(train_loss, train_acc))
        global_net2 = copy.deepcopy(global_net)
        # Verify
        print("Start verification")
        excluded = []
        for k in range(len(clients)):
            params = copy.deepcopy(new_parameters)
            rbs = all_blinding_factors[k]
            for layer_name in client_parameters:
                params[layer_name]['weight'] -= rbs[layer_name]['weight']
                params[layer_name]['bias'] -= rbs[layer_name]['bias']
            global_net2.apply_parameters(params)
            loss, _ = global_net2.evaluate(train_dataset, device)
            if loss < train_loss:
                excluded.append(k)
                print("Exclude client {}".format(k))
        if len(excluded) > 0:
            params = copy.deepcopy(new_parameters)
            for k in excluded:
                rbs = all_blinding_factors[k]
                for layer_name in client_parameters:
                    params[layer_name]['weight'] -= rbs[layer_name]['weight']
                    params[layer_name]['bias'] -= rbs[layer_name]['bias']
            global_net.apply_parameters(params)
            train_loss, train_acc = global_net.evaluate(train_dataset, device)

        print("Upload partial model to blockchain")
        h = save_current_params(taskId, i+1, global_net.get_parameters())
        payload = "{}{}{}".format(taskId, i+1, h)
        sig = sign_msg(pvkeys[0], payload)
        driver.setPartialModelHash(taskId, i+1, h, sig)
        time.sleep(3)

        train_loss, train_acc = global_net.evaluate(train_dataset, device)
        dev_loss, dev_acc = global_net.evaluate(dev_dataset, device)
        print('After round {}, train_loss = {}, dev_loss = {}, dev_acc = {}\n'.format(i + 1, round(train_loss, 4),
            round(dev_loss, 4), round(dev_acc, 4)))


if __name__ == '__main__':
    if len(sys.argv) > 1:
        if sys.argv[1] == "init":
            init()
        elif sys.argv[1] == "genkeys":
            genkeys()
        elif sys.argv[1] == "testsig":
            sign_msg_test("Test ECDSA Signature")
        else:
            print("Unknown argument: {}".format(sys.argv[1]))
    else:
        runfl()
