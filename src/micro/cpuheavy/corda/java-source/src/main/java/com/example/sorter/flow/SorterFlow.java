package com.example.sorter.flow;

import co.paralleluniverse.fibers.Suspendable;
import com.example.sorter.schema.SorterSchemaV1;
import com.example.sorter.contract.SorterContract;
import com.example.sorter.state.SorterState;
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

import static net.corda.core.contracts.ContractsDSL.requireThat;

/**
 * These flows have deliberately been implemented by using only the call() method for ease of understanding. In
 * practice we would recommend splitting up the various stages of the flow into sub-routines.
 * <p>
 * All methods called within the [FlowLogic] sub-class need to be annotated with the @Suspendable annotation.
 */
public class SorterFlow {

    static private final Logger logger = LoggerFactory.getLogger(SorterFlow.class);

    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<SignedTransaction> {

        private final int valueListSize;
        private final String sorterStateId;
        private final Party otherParty;
        private final int contractCommandId;

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

        public Initiator(int valueListSize, int contractCommandId, Party otherParty) {
            this.valueListSize = valueListSize;
            this.contractCommandId = contractCommandId;
            this.otherParty = otherParty;
            this.sorterStateId = null;
        }

        public Initiator(String sorterStateId, int contractCommandId, Party otherParty) {
            this.valueListSize = 0;
            this.contractCommandId = contractCommandId;
            this.otherParty = otherParty;
            this.sorterStateId = sorterStateId;
        }

        @Override
        public ProgressTracker getProgressTracker() {
            return progressTracker;
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

            SorterState sorterState = null;
            StateAndRef<SorterState> sorterStateAndRef = null;
            Command<CommandData> txCommand;
            final TransactionBuilder txBuilder;

            if (contractCommandId == SorterContract.Commands.Create.CMD_ID) {
                if (valueListSize < 2)
                    throw new FlowException("Invalid list size in state: " + valueListSize);
                sorterState = new SorterState(valueListSize, null, me, otherParty, new UniqueIdentifier());
                sorterState.genReverseValues();
                txCommand = new Command<>(
                        new SorterContract.Commands.Create(),
                        ImmutableList.of(
                                sorterState.getContractee().getOwningKey(),
                                sorterState.getContractor().getOwningKey()));
                txBuilder = new TransactionBuilder(notary)
                        .addOutputState(sorterState, SorterContract.CONTRACT_ID)
                        .addCommand(txCommand);
            } else if (contractCommandId == SorterContract.Commands.CreateAndSort.CMD_ID) {
                if (valueListSize < 2)
                    throw new FlowException("Invalid list size in state: " + valueListSize);
                sorterState = new SorterState(valueListSize, null, me, otherParty, new UniqueIdentifier());
                sorterState.genReverseValues();
                sorterState.sort();
                txCommand = new Command<>(
                        new SorterContract.Commands.CreateAndSort(),
                        ImmutableList.of(sorterState.getContractee().getOwningKey(), sorterState.getContractor().getOwningKey()));
                txBuilder = new TransactionBuilder(notary)
                        .addOutputState(sorterState, SorterContract.CONTRACT_ID)
                        .addCommand(txCommand);
            } else if (contractCommandId == SorterContract.Commands.Sort.CMD_ID) {
                try {
                    Field stateId = SorterSchemaV1.PersistentSorterState.class.getDeclaredField("linearId");
                    CriteriaExpression index = Builder.equal(stateId, sorterStateId);
                    QueryCriteria myCriteria = new QueryCriteria.VaultCustomQueryCriteria(index);
                    sorterStateAndRef = getServiceHub().getVaultService().queryBy(SorterState.class, myCriteria).getStates().get(0);
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
                /*
                List<StateAndRef<SorterState>> states = getServiceHub().getVaultService().queryBy(SorterState.class).getStates();
                for (StateAndRef<SorterState> state : states) {
                    if (state.getState().getData().getLinearId().getId().toString().equals(sorterStateId)) {

                        sorterStateAndRef = state;
                        break;
                    }
                }
                */
                if (sorterStateAndRef == null)
                    throw new FlowException("State not found: " + sorterStateId);

                sorterState = SorterState.copyFrom(sorterStateAndRef.getState().getData());
                sorterState.sort();
                logger.info("Sorter state is sorted? " + sorterState.isSorted());

                txCommand = new Command<>(
                        new SorterContract.Commands.Sort(),
                        ImmutableList.of(sorterState.getContractee().getOwningKey(), sorterState.getContractor().getOwningKey()));

                txBuilder = new TransactionBuilder(notary)
                        .addInputState(sorterStateAndRef)
                        .addOutputState(sorterState, SorterContract.CONTRACT_ID)
                        .addCommand(txCommand);
            } else
                throw new FlowException("Invalid contract command: " + contractCommandId);

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
                        require.using("This must be a Sorter transaction.", output instanceof SorterState);
                        return null;
                    });
                }
            }

            return subFlow(new SignTxFlow(otherPartyFlow, SignTransactionFlow.Companion.tracker()));
        }
    }
}
