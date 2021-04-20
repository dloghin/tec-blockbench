/*
 * Copyright IBM Corp. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

'use strict';

const { Gateway, Wallets, DefaultEventHandlerStrategies  } = require('fabric-network');
const fs = require('fs');
const path = require('path');
const express = require('express');
const bodyParser = require('body-parser');


const argsLen = process.argv.length;

function helper() {
    console.error("Expected usage: ")
    console.error("\tnode txn-server.js <> <channelName> <contractName> [open_loop|closed_loop] <port>")
    console.error("\tThe 'open_loop' indicates the invocation will return once the transaction is submitted to orderers. The 'closed_loop' will block until peers finish validation. Their configuration may impact on invocation transactions, not on queries. ")

}
if ( argsLen <= 7) {
    console.error(`Too few arguments, expect 8`);
    helper();
    process.exit(1);
}
const networkDir = process.argv[2];
const org_id = parseInt(process.argv[3])
const channelName = process.argv[4];
const contractName = process.argv[5];
const isOpenLoopMode = process.argv[6] == "open_loop";
const port = Number(process.argv[7]);

async function getChannel(channelName, contractName) {
    try {
        // load the network configuration
        const profile_path = path.resolve(networkDir, "crypto_config", "peerOrganizations", `org${org_id}.example.com`, "connection_profile.json");
        const connection_profile = JSON.parse(fs.readFileSync(profile_path, 'utf8'));

        const cert_path = path.join(networkDir, "crypto_config", "peerOrganizations", `org${org_id}.example.com`, "users", `Admin@org${org_id}.example.com`, "msp", "signcerts", `Admin@org${org_id}.example.com-cert.pem`);
        const key_path = path.join(networkDir, "crypto_config", "peerOrganizations", `org${org_id}.example.com`, "users", `Admin@org${org_id}.example.com`, "msp", "keystore", "priv_sk");

        const x509Identity = {
            credentials: {
                certificate: fs.readFileSync(cert_path).toString(),
                privateKey: fs.readFileSync(key_path).toString(),
            },
            mspId: `Org${org_id}MSP`,
            type: 'X.509',
        };   


        // Create a new gateway for connecting to our peer node.
        var mode;
        if (isOpenLoopMode) {
            mode = DefaultEventHandlerStrategies.NONE;
        } else {
            mode = DefaultEventHandlerStrategies.MSPID_SCOPE_ALLFORTX
        }
        const gateway = new Gateway();
        await gateway.connect(connection_profile, 
            { 
            identity:x509Identity ,
            discovery: { enabled: true, asLocalhost: false},
            eventHandlerOptions: {
                strategy: mode
            }  
        });

        // Get the network (channel) our contract is deployed to.
        const network = await gateway.getNetwork(channelName);

        // Get the contract from the network.
        const contract = network.getContract(contractName);
        console.log(`Contract ${contractName} on Channel ${channelName} has been setup... }`);
        return contract;

    } catch (error) {
        console.error(`Failed to set up the contract and channel: ${error}`);
        process.exit(1);
    }
}

getChannel(channelName, contractName).then((contract)=>{
    const app = express();

    app.listen(port, () => {
        console.log(`Server running on port ${port}. Is the open-loop mode? ${isOpenLoopMode}`);
    })

    app.use(bodyParser.json());

    app.post("/invoke", (req, res) => { 
        var txn;
        const funcName = req.body["function"];
        const args = req.body["args"];
        console.log(`Receive funcName: ${funcName}, args: ${args}`);
        var start; 
        new Promise((resolve, reject)=>{
            txn = contract.createTransaction(funcName);
            start = new Date();
            resolve(txn.submit(...args));
        }).then(()=>{
            var end = new Date() - start
            const txnID = txn.getTransactionId();
            res.json({"status": "0", "txnID": txnID, "latency_ms": end});
        }).catch((error)=>{
            console.error(`Failed to invoke with error: ${error}`);
            res.json({"status": "1", "message": error.message});
        });
    });

    app.get("/query", (req, res) => { 
        const funcName = req.query.function;
        const args = req.query.args.split(',');
        console.log(`Receive funcName: ${funcName}, args: ${args}`);
        var start; 
        new Promise((resolve, reject)=>{
            start = new Date();
            resolve(contract.evaluateTransaction(funcName, ...args));
        }).then((result)=>{
            var end = new Date() - start
            res.json({"status": "0", "result": result.toString(), "latency_ms": end});
        }).catch((error)=>{
            console.error(`Failed to query with error: ${error}`);
            res.json({"status": "1", "message": error.message});
        });
    });
})