package com.example.sorter.flow;

import com.example.sorter.contract.SorterContract;
import com.example.sorter.state.SorterState;
import com.google.common.collect.ImmutableList;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.TransactionState;
import net.corda.core.contracts.TransactionVerificationException;
import net.corda.core.flows.FlowException;
import net.corda.core.transactions.SignedTransaction;
import net.corda.testing.node.MockNetwork;
import net.corda.testing.node.StartedMockNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.List;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SorterFlowTests {
    private MockNetwork network;
    private StartedMockNode a;
    private StartedMockNode b;

    @Before
    public void setup() {
        network = new MockNetwork(ImmutableList.of("com.example.sorter.contract"));
        a = network.createPartyNode(null);
        b = network.createPartyNode(null);
        // For real nodes this happens automatically, but we have to manually register the flow for tests.
        for (StartedMockNode node : ImmutableList.of(a, b)) {
            node.registerInitiatedFlow(SorterFlow.Acceptor.class);
        }
        network.runNetwork();
    }

    @After
    public void tearDown() {
        network.stopNodes();
    }

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void flowRejectsInvalidSorters() throws Exception {
        // cannot have negative size
        SorterFlow.Initiator flow = new SorterFlow.Initiator(-1, SorterContract.Commands.Create.CMD_ID,  b.getInfo().getLegalIdentities().get(0));
        CordaFuture<SignedTransaction> future = a.startFlow(flow);
        network.runNetwork();

        exception.expectCause(instanceOf(FlowException.class));
        future.get();
    }

    @Test
    public void signedTransactionReturnedByTheFlowIsSignedByTheInitiator() throws Exception {
        Integer size = 1000;
        SorterFlow.Initiator flow = new SorterFlow.Initiator(size, SorterContract.Commands.Create.CMD_ID, b.getInfo().getLegalIdentities().get(0));
        CordaFuture<SignedTransaction> future = a.startFlow(flow);
        network.runNetwork();

        SignedTransaction signedTx = future.get();
        signedTx.verifySignaturesExcept(b.getInfo().getLegalIdentities().get(0).getOwningKey());
    }

    @Test
    public void signedTransactionReturnedByTheFlowIsSignedByTheAcceptor() throws Exception {
        Integer size = 1000;
        SorterFlow.Initiator flow = new SorterFlow.Initiator(size, SorterContract.Commands.Create.CMD_ID, b.getInfo().getLegalIdentities().get(0));
        CordaFuture<SignedTransaction> future = a.startFlow(flow);
        network.runNetwork();

        SignedTransaction signedTx = future.get();
        signedTx.verifySignaturesExcept(a.getInfo().getLegalIdentities().get(0).getOwningKey());
    }

    @Test
    public void flowRecordsATransactionInBothPartiesTransactionStorages() throws Exception {
        Integer size = 1000;
        SorterFlow.Initiator flow = new SorterFlow.Initiator(size, SorterContract.Commands.Create.CMD_ID, b.getInfo().getLegalIdentities().get(0));
        CordaFuture<SignedTransaction> future = a.startFlow(flow);
        network.runNetwork();
        SignedTransaction signedTx = future.get();

        // We check the recorded transaction in both vaults.
        for (StartedMockNode node : ImmutableList.of(a, b)) {
            assertEquals(signedTx, node.getServices().getValidatedTransactions().getTransaction(signedTx.getId()));
        }
    }

    @Test
    public void recordedTransactionHasNoInputsAndASingleOutput() throws Exception {
        Integer size = 1000;
        SorterFlow.Initiator flow = new SorterFlow.Initiator(size, SorterContract.Commands.CreateAndSort.CMD_ID, b.getInfo().getLegalIdentities().get(0));
        CordaFuture<SignedTransaction> future = a.startFlow(flow);
        network.runNetwork();
        SignedTransaction signedTx = future.get();

        // We check the recorded transaction in both vaults.
        for (StartedMockNode node : ImmutableList.of(a, b)) {
            SignedTransaction recordedTx = node.getServices().getValidatedTransactions().getTransaction(signedTx.getId());
            List<TransactionState<ContractState>> txOutputs = recordedTx.getTx().getOutputs();
            assert (txOutputs.size() == 1);

            SorterState recordedState = (SorterState) txOutputs.get(0).getData();
            assertEquals((long)recordedState.getSize(), (long)size);
            assertTrue(recordedState.isSorted());
            assertEquals(recordedState.getContractee(), a.getInfo().getLegalIdentities().get(0));
            assertEquals(recordedState.getContractor(), b.getInfo().getLegalIdentities().get(0));
        }
    }

    @Test
    public void flowRecordsTheCorrectStatesInBothPartiesVaults() throws Exception {
        Integer size = 1000;
        SorterFlow.Initiator flow = new SorterFlow.Initiator(size, SorterContract.Commands.CreateAndSort.CMD_ID, b.getInfo().getLegalIdentities().get(0));
        CordaFuture<SignedTransaction> future = a.startFlow(flow);
        network.runNetwork();
        future.get();

        // We check the recorded states in both vaults.
        for (StartedMockNode node : ImmutableList.of(a, b)) {
            node.transaction(() -> {
                List<StateAndRef<SorterState>> sorters = node.getServices().getVaultService().queryBy(SorterState.class).getStates();
                assertEquals(1, sorters.size());
                SorterState recordedState = sorters.get(0).getState().getData();
                assertEquals((long)recordedState.getSize(), (long)size);
                assertTrue(recordedState.isSorted());
                assertEquals(recordedState.getContractee(), a.getInfo().getLegalIdentities().get(0));
                assertEquals(recordedState.getContractor(), b.getInfo().getLegalIdentities().get(0));
                return null;
            });
        }
    }
}
