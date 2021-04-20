/*
 * Copyright IBM Corp. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

'use strict';

const { Gateway, DefaultEventHandlerStrategies } = require('fabric-network');
const path = require('path');
const fs = require('fs');

async function main() {
    try {
        // load the network configuration
        if (process.argv.length < 8) {
            console.error("Insufficient parameters.");
            return 1;
        }
        let network_dir = path.join(process.cwd(), process.argv[2])
        let org_id = parseInt(process.argv[3])
        let is_open_loop = process.argv[4] === "open_loop"
        let channel_name = process.argv[5]
        let cc_name = process.argv[6]
        let invoke_fcn = process.argv[7]
        let invoke_args = process.argv.slice(8)

        // console.log(`channel_name: ${channel_name}`)
        // console.log(`cc_name: ${cc_name}`)
        // console.log(`invoke_fcn: ${invoke_fcn}`)
        // console.log(`invoke_args: ${invoke_args}`)

        const profile_path = path.resolve(network_dir, "crypto_config", "peerOrganizations", `org${org_id}.example.com`, "connection_profile.json");
        const connection_profile = JSON.parse(fs.readFileSync(profile_path, 'utf8'));

        // Check to see if we've already enrolled the user.
        const cert_path = path.join(network_dir, "crypto_config", "peerOrganizations", `org${org_id}.example.com`, "users", `Admin@org${org_id}.example.com`, "msp", "signcerts", `Admin@org${org_id}.example.com-cert.pem`);
        const key_path = path.join(network_dir, "crypto_config", "peerOrganizations", `org${org_id}.example.com`, "users", `Admin@org${org_id}.example.com`, "msp", "keystore", "priv_sk");

        const x509Identity = {
            credentials: {
                certificate: fs.readFileSync(cert_path).toString(),
                privateKey: fs.readFileSync(key_path).toString(),
            },
            mspId: `Org${org_id}MSP`,
            type: 'X.509',
        };        

        // Wait for the peer in the org commits this transaction
        let op = DefaultEventHandlerStrategies.MSPID_SCOPE_ALLFORTX;
        if (is_open_loop) {
            // Wait for the orderer to ack the receipt of the transaction.
            // Refer to https://hyperledger.github.io/fabric-sdk-node/release-2.2/module-fabric-network.html#.DefaultEventHandlerStrategies for detailed differences.
            op = DefaultEventHandlerStrategies.NONE;
        }
        const gateway_option = {
            identity:x509Identity ,
            discovery: { enabled: true, asLocalhost: false},
            eventHandlerOptions: {
                strategy: op 
            }  
        };

        const gateway = new Gateway();
        await gateway.connect(connection_profile, gateway_option);

        const network = await gateway.getNetwork(channel_name);
        const contract = network.getContract(cc_name);

        const start = Date.now();
        const txn = contract.createTransaction(invoke_fcn);
        await txn.submit(...invoke_args);
        const millis = Date.now() - start;
        const txnID = txn.getTransactionId();
        if (is_open_loop) {
            console.log(`Txn ${txnID} is broadcasted to orderers. Duration (ms) = ${millis}`);
        } else {
            console.log(`Txn ${txnID} is committed on the peer of Org${org_id}. Duration (ms) = ${millis}`);
        }

        // Disconnect from the gateway.
        await gateway.disconnect();
        
    } catch (error) {
        console.error(`Failed to invoke transaction: ${error}`);
        process.exit(1);
    }
}

main();
