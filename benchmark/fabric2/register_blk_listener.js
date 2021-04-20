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
        if (process.argv.length < 5) {
            console.error("Insufficient parameters.")
        }
        let network_dir = path.join(process.cwd(), process.argv[2])
        let org_id = parseInt(process.argv[3])
        let channel_name = process.argv[4]

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

        const gateway_option = {
            identity:x509Identity ,
            discovery: { enabled: true, asLocalhost: false},
        };

        const gateway = new Gateway();
        await gateway.connect(connection_profile, gateway_option);

        const network = await gateway.getNetwork(channel_name);
        var validBlkTxns = {};
        var invalidBlkTxns = {};

        const listener = async (event) => {
            try {
                const blkNum = "" + event.blockNumber; //conver to str
                const block = event.blockData;
                validBlkTxns[blkNum] = [];
                invalidBlkTxns[blkNum] = [];
                let tx_filters = block.metadata.metadata[2]
                for (var index = 0; index < block.data.data.length; index++) {
                    var channel_header = block.data.data[index].payload.header.channel_header;
                    if (tx_filters[index] === 0) { 
                        // this txn passes the validation and is committed.
                        validBlkTxns[blkNum].push(channel_header.tx_id)
                    } else {
                        invalidBlkTxns[blkNum].push(channel_header.tx_id)
                    }
                }
                console.log(`\tBlock ${blkNum} has valid txns [${validBlkTxns[blkNum]}] and invalid txns [${invalidBlkTxns[blkNum]}]. `);
    
            } catch (error) {
                console.error(`Failed to listen for blocks: ${error}`);
            }
        };
        console.log(`Register the block listener on the peer of Org${org_id}`);
        // undefined implies listening from the current block.
        // Details at https://hyperledger.github.io/fabric-sdk-node/release-2.2/module-fabric-network.ListenerOptions.html
        return network.addBlockListener(listener, {startBlock: undefined});
    } catch (error) {
        console.error(`Failed to regiter block listener: ${error}`);
        process.exit(1);
    }
}

main().then(()=>{
    // A random large time out to keep the process alive. 
    setTimeout(function() { }, 300000);
});
