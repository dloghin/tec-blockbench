package com.example.ioheavy.flow;

import co.paralleluniverse.fibers.Suspendable;
import com.example.ioheavy.contract.IOHeavyContract;
import com.example.ioheavy.schema.StringKVSchema;
import com.example.ioheavy.schema.StringKVSchemaV1;
import com.example.ioheavy.state.StringKVState;
import com.example.kv.KVState;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import net.corda.core.contracts.*;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.node.services.vault.Builder;
import net.corda.core.node.services.vault.CriteriaExpression;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import net.corda.core.utilities.ProgressTracker.Step;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static net.corda.core.contracts.ContractsDSL.requireThat;

/**
 * These flows have deliberately been implemented by using only the call() method for ease of understanding. In
 * practice we would recommend splitting up the various stages of the flow into sub-routines.
 * <p>
 * All methods called within the [FlowLogic] sub-class need to be annotated with the @Suspendable annotation.
 */
public class IOHeavyFlow {

    static private final Logger logger = LoggerFactory.getLogger(IOHeavyFlow.class);

    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<SignedTransaction> {

        private final int startKey;
        private final int numKeys;
        private final int contractCommandId;
        private final Party otherParty;

        private static final String ALPHABET = "abcdefghijklmnopqrstuvwxy#$%^&*()_+[]{}|;:,./<>?`~abcdefghijklmnopqrstuvwxy#$%^&*()_+[]{}|;:,./<>?`~abcdefghijklmnopqrstuvwxy#$%^&*()_+[]{}|;:,./<>?`~";
        private static final String scanKey = "scan";

        private final Step GENERATING_TRANSACTION = new Step("Generating transaction.");
        private final Step VERIFYING_TRANSACTION = new Step("Verifying contract constraints.");
        private final Step SIGNING_TRANSACTION = new Step("Signing transaction with our private key.");
        private final Step GATHERING_SIGS = new Step("Gathering the counterparty's signature.") {
            @Override
            public ProgressTracker childProgressTracker() {
                return CollectSignaturesFlow.Companion.tracker();
            }
        };
        private final Step FINALISING_TRANSACTION =
                new Step("Obtaining notary signature and recording transaction.") {
                    @Override
                    public ProgressTracker childProgressTracker() {
                        return FinalityFlow.Companion.tracker();
                    }
                };

        // The progress tracker checkpoints each stage of the flow and outputs the specified messages when each
        // checkpoint is reached in the code. See the 'progressTracker.currentStep' expressions within the call()
        // function.
        private final ProgressTracker progressTracker = new ProgressTracker(
                GENERATING_TRANSACTION,
                VERIFYING_TRANSACTION,
                SIGNING_TRANSACTION,
                GATHERING_SIGS,
                FINALISING_TRANSACTION
        );

        public Initiator(int startKey, int numKeys, int contractCommandId, Party otherParty) {
            this.startKey = startKey;
            this.numKeys = numKeys;
            this.contractCommandId = contractCommandId;
            this.otherParty = otherParty;
        }

        @Override
        public ProgressTracker getProgressTracker() {
            return progressTracker;
        }


        private String genKey(int k) {
            return String.format("%020d", k);
        }

        private String genVal(int k) {
            return ALPHABET.substring(k % 50, k%50 + 100);
        }

        /**
         * The flow logic is encapsulated within the call() method.
         */
        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            // Obtain a reference to the notary we want to use.
            final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

            // Stage 1.
            progressTracker.setCurrentStep(GENERATING_TRANSACTION);
            // Generate an unsigned transaction.
            Party me = getServiceHub().getMyInfo().getLegalIdentities().get(0);

            Command<CommandData> txCommand;
            final TransactionBuilder txBuilder;
            List<Party> participants = new ArrayList<>(2);
            participants.add(me);
            participants.add(otherParty);

            if (startKey < 0 || numKeys < 1)
                throw new FlowException("Invalid start key or number of keys: (" + startKey + ", " + numKeys + ")");

            switch (contractCommandId) {
                case IOHeavyContract.Commands.Write.CMD_ID:
                    txCommand = new Command<>(
                            new IOHeavyContract.Commands.Write(numKeys),
                            ImmutableList.of(me.getOwningKey(), otherParty.getOwningKey()));
                    txBuilder = new TransactionBuilder(notary).addCommand(txCommand);
                    for (int k = startKey; k < startKey + numKeys; k++) {
                        StringKVState state = new StringKVState(genKey(k), genVal(k), participants, new UniqueIdentifier());
                        txBuilder.addOutputState(state, IOHeavyContract.CONTRACT_ID);
                    }
                    break;
                case IOHeavyContract.Commands.Scan.CMD_ID:
                    txCommand = new Command<>(
                            new IOHeavyContract.Commands.Scan(),
                            ImmutableList.of(me.getOwningKey(), otherParty.getOwningKey()));
                    txBuilder = new TransactionBuilder(notary).addCommand(txCommand);
                    for (int k = startKey; k < startKey + numKeys; k++) {
                        boolean found = false;
                        try {
                            Field key = StringKVSchemaV1.PersistentStringKVState.class.getDeclaredField("key");
                            CriteriaExpression index = Builder.equal(key, genKey(k));
                            QueryCriteria myCriteria = new QueryCriteria.VaultCustomQueryCriteria(index);
                            StateAndRef<StringKVState> kvStateAndRef = getServiceHub().getVaultService().queryBy(StringKVState.class, myCriteria).getStates().get(0);
                            if (kvStateAndRef.getState().getData().getValue().equals(genVal(k)))
                                found = true;
                        }
                        catch (NoSuchFieldException e1) {
                            logger.warn(e1.getMessage());
                        }
                        catch (IndexOutOfBoundsException e2) {
                            logger.warn(e2.getMessage());
                        }
                        catch (Exception e3) {
                            logger.warn(e3.getMessage());
                        }
                        if (!found)
                            throw new FlowException("KVState not found: " + genKey(k));
                    }
                    StringKVState state = new StringKVState(
                            scanKey,
                            "scanned " + numKeys + " states starting with key " + genKey(startKey),
                            participants, new UniqueIdentifier());
                    txBuilder.addOutputState(state, IOHeavyContract.CONTRACT_ID);
                    break;
                default:
                    throw new FlowException("Invalid contract command: " + contractCommandId);
            }

            // Stage 2.
            progressTracker.setCurrentStep(VERIFYING_TRANSACTION);
            // Verify that the transaction is valid.
            txBuilder.verify(getServiceHub());

            // Stage 3.
            progressTracker.setCurrentStep(SIGNING_TRANSACTION);
            // Sign the transaction.
            final SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(txBuilder);

            // Stage 4.
            progressTracker.setCurrentStep(GATHERING_SIGS);
            // Send the state to the counterparty, and receive it back with their signature.
            FlowSession otherPartySession = initiateFlow(otherParty);
            final SignedTransaction fullySignedTx = subFlow(
                    new CollectSignaturesFlow(
                            partSignedTx,
                            ImmutableSet.of(otherPartySession),
                            CollectSignaturesFlow.Companion.tracker()));

            // Stage 5.
            progressTracker.setCurrentStep(FINALISING_TRANSACTION);
            // Notarise and record the transaction in both parties' vaults.
            return subFlow(new FinalityFlow(fullySignedTx));
        }
    }

    @InitiatedBy(Initiator.class)
    public static class Acceptor extends FlowLogic<SignedTransaction> {

        private final FlowSession otherPartyFlow;

        public Acceptor(FlowSession otherPartyFlow) {
            this.otherPartyFlow = otherPartyFlow;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            class SignTxFlow extends SignTransactionFlow {
                private SignTxFlow(FlowSession otherPartyFlow, ProgressTracker progressTracker) {
                    super(otherPartyFlow, progressTracker);
                }

                @Override
                protected void checkTransaction(SignedTransaction stx) {
                    requireThat(require -> {
                        ContractState output = stx.getTx().getOutputs().get(0).getData();
                        require.using("This must be a Sorter transaction.", output instanceof StringKVState);
                        return null;
                    });
                }
            }

            return subFlow(new SignTxFlow(otherPartyFlow, SignTransactionFlow.Companion.tracker()));
        }
    }
}
