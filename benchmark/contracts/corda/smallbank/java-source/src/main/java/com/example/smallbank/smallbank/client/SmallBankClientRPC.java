package com.example.smallbank.smallbank.client;

import com.example.smallbank.smallbank.state.SmallBankState;
import net.corda.client.rpc.CordaRPCClient;
import net.corda.client.rpc.CordaRPCClientConfiguration;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.messaging.DataFeed;
import net.corda.core.node.services.Vault;
import net.corda.core.utilities.NetworkHostAndPort;
import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.util.concurrent.ExecutionException;

/**
 * Demonstration of using the CordaRPCClient to connect to a Corda Node and
 * steam some State data from the node.
 */
public class SmallBankClientRPC {
    private static final Logger logger = LoggerFactory.getLogger(SmallBankClientRPC.class);

    private static void logState(StateAndRef<SmallBankState> state) {
        logger.info("{}", state.getState().getData());
    }

    public static void main(String[] args) throws ActiveMQException, InterruptedException, ExecutionException {
        if (args.length != 1) {
            throw new IllegalArgumentException("Usage: SmallBankClientRPC <node address>");
        }

        final NetworkHostAndPort nodeAddress = NetworkHostAndPort.parse(args[0]);
        final CordaRPCClient client = new CordaRPCClient(nodeAddress, CordaRPCClientConfiguration.DEFAULT);

        // Can be amended in the com.example.Main file.
        final CordaRPCOps proxy = client.start("user1", "test").getProxy();

        // Grab all existing and future IOU states in the vault.
        final DataFeed<Vault.Page<SmallBankState>, Vault.Update<SmallBankState>> dataFeed = proxy.vaultTrack(SmallBankState.class);
        final Vault.Page<SmallBankState> snapshot = dataFeed.getSnapshot();
        final Observable<Vault.Update<SmallBankState>> updates = dataFeed.getUpdates();

        // Log the 'placed' IOUs and listen for new ones.
        snapshot.getStates().forEach(SmallBankClientRPC::logState);
        updates.toBlocking().subscribe(update -> update.getProduced().forEach(SmallBankClientRPC::logState));
    }
}
