package com.example.analytic.client;

import com.example.analytic.state.BankAccount;
import com.example.analytic.state.BankTransaction;
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
public class AnalyticsClientRPC {
    private static final Logger logger = LoggerFactory.getLogger(AnalyticsClientRPC.class);

    private static void logBankAccounts(StateAndRef<BankAccount> state) {
        logger.info("{}", state.getState().getData());
    }

    private static void logBankTransactions(StateAndRef<BankTransaction> state) {
        logger.info("{}", state.getState().getData());
    }

    public static void main(String[] args) throws ActiveMQException, InterruptedException, ExecutionException {
        if (args.length != 1) {
            throw new IllegalArgumentException("Usage: AnalyticsClientRPC <node address>");
        }

        final NetworkHostAndPort nodeAddress = NetworkHostAndPort.parse(args[0]);
        final CordaRPCClient client = new CordaRPCClient(nodeAddress, CordaRPCClientConfiguration.DEFAULT);

        // Can be amended in the com.example.Main file.
        final CordaRPCOps proxy = client.start("user1", "test").getProxy();

        // Grab all existing and future states in the vault.
        final DataFeed<Vault.Page<BankAccount>, Vault.Update<BankAccount>> dataFeed1 = proxy.vaultTrack(BankAccount.class);
        final Vault.Page<BankAccount> snapshot1 = dataFeed1.getSnapshot();
        final Observable<Vault.Update<BankAccount>> updates1 = dataFeed1.getUpdates();

        // Log the 'placed' states and listen for new ones.
        snapshot1.getStates().forEach(AnalyticsClientRPC::logBankAccounts);
        updates1.toBlocking().subscribe(update -> update.getProduced().forEach(AnalyticsClientRPC::logBankAccounts));

        // Grab all existing and future states in the vault.
        final DataFeed<Vault.Page<BankTransaction>, Vault.Update<BankTransaction>> dataFeed2 = proxy.vaultTrack(BankTransaction.class);
        final Vault.Page<BankTransaction> snapshot2 = dataFeed2.getSnapshot();
        final Observable<Vault.Update<BankTransaction>> updates2 = dataFeed2.getUpdates();

        // Log the 'placed' states and listen for new ones.
        snapshot2.getStates().forEach(AnalyticsClientRPC::logBankTransactions);
        updates2.toBlocking().subscribe(update -> update.getProduced().forEach(AnalyticsClientRPC::logBankTransactions));
    }
}
