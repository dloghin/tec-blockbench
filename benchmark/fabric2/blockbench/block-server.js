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
if ( argsLen <= 5) {
    console.error(`Too few arguments, expect 6`);
    console.error("Expected usage: ")
    console.error("\tnode block-server.js <channelName> <port>")
    process.exit(1);
}

const networkDir = process.argv[2];
const org_id = parseInt(process.argv[3])
const channelName = process.argv[4];
const port = Number(process.argv[5]);
var blkTxns = {};
var height = 0;

async function getChannel(channelName) {
    try {
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
        const gateway = new Gateway();
        await gateway.connect(connection_profile, 
            { 
            identity:x509Identity ,
            discovery: { enabled: true, asLocalhost: false},
        });

        // Get the network (channel) our contract is deployed to.
        const network = await gateway.getNetwork(channelName);
        console.log(`Channel ${channelName} has been setup... }`);
        return network;

    } catch (error) {
        console.error(`Failed to set up the contract and channel: ${error}`);
        process.exit(1);
    }
}

getChannel(channelName).then((network)=>{
    const listener = async (event) => {
        try {
            height = Number(event.blockNumber) + 1;
            const blkNum = "" + event.blockNumber; //conver to str
            const block = event.blockData;
            blkTxns[blkNum] = [];
            let tx_filters = block.metadata.metadata[2]
            for (var index = 0; index < block.data.data.length; index++) {
                var channel_header = block.data.data[index].payload.header.channel_header;
                if (tx_filters[index] === 0) {
                    blkTxns[blkNum].push(channel_header.tx_id)
                }
            }
            console.log(`Block ${blkNum} has txns [${blkTxns[blkNum]}]. `);

        } catch (error) {
            console.error(`Failed to listen for blocks: ${error}`);
        }
    };
    return network.addBlockListener(listener, {startBlock: 1});
}).then(()=>{
    const app = express();

    app.listen(port, () => {
        console.log(`Server running on port ${port}`);
    })

    // app.use(bodyParser.json());

    app.get("/block", (req, res) => { 
        const blkNum = "" + req.query.num; //convert to string
        const txns = blkTxns[blkNum];
        if (txns === undefined) {
            res.json({"status": "1", "message": "Block " + blkNum + " does not exist. "});
        } else {
            res.json({"status": "0", "txns": txns});
        }
    });

    app.get("/height", (req, res) => { 
        res.json({"status": "0", "height": "" + height});
    });
}).catch((error)=>{
    console.error(`Failed to set up the contract and channel: ${error}`);
    process.exit(1);

})