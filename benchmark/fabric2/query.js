/*
 * Copyright IBM Corp. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

'use strict';

const { Gateway} = require('fabric-network');
const path = require('path');
const fs = require('fs');


async function main() {
    try {
        // load the network configuration
        if (process.argv.length < 7) {
            console.error("Insufficient parameters.");
            return 1;        
        }
        let network_dir = path.join(process.cwd(), process.argv[2])
        let org_id = parseInt(process.argv[3])
        let channel_name = process.argv[4]
        let cc_name = process.argv[5]
        let query_fcn = process.argv[6]
        let query_args = process.argv.slice(7)

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
        const contract = network.getContract(cc_name);

        const result = await contract.evaluateTransaction(query_fcn, ...query_args);
        console.log(`The query has been evaluated on the peer of Org${org_id}, result is: ${result.toString()}`);

        await gateway.disconnect();
        
    } catch (error) {
        console.error(`Failed to query: ${error}`);
        process.exit(1);
    }
}

main();
