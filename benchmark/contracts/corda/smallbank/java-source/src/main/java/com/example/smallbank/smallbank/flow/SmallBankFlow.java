package com.example.smallbank.smallbank.flow;

import co.paralleluniverse.fibers.Suspendable;
import com.example.smallbank.smallbank.contract.SmallBankContract;
import com.example.smallbank.smallbank.schema.SmallBankSchemaV1;
import com.example.smallbank.smallbank.state.SmallBankState;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import net.corda.core.contracts.*;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.node.services.VaultService;
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
public class SmallBankFlow {

    static private final Logger logger = LoggerFactory.getLogger(SmallBankFlow.class);

    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<SignedTransaction> {

        private final int account1;
        private final int account2;
        private final int amount;
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

        public Initiator(int account1, int account2, int amount, int contractCommandId, Party otherParty) {
            this.account1 = account1;
            this.account2 = account2;
            this.contractCommandId = contractCommandId;
            this.otherParty = otherParty;
            this.amount = amount;
        }

        @Override
        public ProgressTracker getProgressTracker() {
            return progressTracker;
        }

        public static StateAndRef<SmallBankState> queryState(VaultService vaultService, CordaRPCOps rpcOps, int tab, int accountId) {
            StateAndRef<SmallBankState> sorterStateAndRef = null;
            try {
                Field tabField = SmallBankSchemaV1.PersistentSmallBankState.class.getDeclaredField("tab");
                Field accountIdField = SmallBankSchemaV1.PersistentSmallBankState.class.getDeclaredField("accountId");
                CriteriaExpression crt1 = Builder.equal(tabField, tab);
                CriteriaExpression crt2 = Builder.equal(accountIdField, accountId);
                QueryCriteria myCriteria1 = new QueryCriteria.VaultCustomQueryCriteria(crt1);
                QueryCriteria myCriteria2 = new QueryCriteria.VaultCustomQueryCriteria(crt2);
                myCriteria1 = myCriteria1.and(myCriteria2);
                if (vaultService != null) {
                    sorterStateAndRef = vaultService.queryBy(SmallBankState.class, myCriteria1).getStates().get(0);
                }
                else {
                    sorterStateAndRef = rpcOps.vaultQueryByCriteria(myCriteria1, SmallBankState.class).getStates().get(0);
                }
            } catch (NoSuchFieldException e1) {
                logger.warn(e1.getMessage());
            } catch (IndexOutOfBoundsException e2) {
                logger.warn(e2.getMessage());
            } catch (Exception e3) {
                logger.warn(e3.getMessage());
            }
            return sorterStateAndRef;
        }

        private StateAndRef<SmallBankState> getState(int tab, int accountId) {
            return queryState(getServiceHub().getVaultService(), null, tab, accountId);
        }

        /*
         * Input states: 2, 1 or 0
         * Output states: 2
         */
        private TransactionBuilder amalgate(int savingsAccountId, int checkingsAccountId, Party me, Party notary) {
            SmallBankState savingsState = null;
            SmallBankState checkingsState = null;
            SmallBankState newSavingsState = null;
            SmallBankState newCheckingsState = null;

            StateAndRef<SmallBankState> checkingsStateAndRef = getState(SmallBankState.CheckingsTab, checkingsAccountId);
            if (checkingsStateAndRef != null && checkingsStateAndRef.getState() != null) {
                checkingsState = checkingsStateAndRef.getState().getData();
            }
            if (checkingsState == null) {
                newCheckingsState = new SmallBankState(me, SmallBankState.CheckingsTab, checkingsAccountId, 0, new UniqueIdentifier());
            } else {
                newCheckingsState = SmallBankState.copyFromAdjustBalance(checkingsState, -checkingsState.getBalance());
            }

            StateAndRef<SmallBankState> savingsStateAndRef = getState(SmallBankState.SavingsTab, savingsAccountId);
            if (savingsStateAndRef != null && savingsStateAndRef.getState() != null) {
                savingsState = savingsStateAndRef.getState().getData();
            }
            if (savingsState == null) {
                if (checkingsState == null) {
                    newSavingsState = new SmallBankState(me, SmallBankState.SavingsTab, savingsAccountId, SmallBankState.DEFAULT_BALANCE, new UniqueIdentifier());
                } else {
                    newSavingsState = new SmallBankState(me, SmallBankState.SavingsTab, savingsAccountId, SmallBankState.DEFAULT_BALANCE + checkingsState.getBalance(), new UniqueIdentifier());
                }
            } else {
                if (checkingsState == null) {
                    newSavingsState = SmallBankState.copyFromAdjustBalance(savingsState, 0);
                } else {
                    newSavingsState = SmallBankState.copyFromAdjustBalance(savingsState, checkingsState.getBalance());
                }
            }

            Command<CommandData> txCommand = new Command<>(
                    new SmallBankContract.Commands.Amalgate(),
                    ImmutableList.of(me.getOwningKey(), otherParty.getOwningKey()));

            TransactionBuilder txBuilder = new TransactionBuilder(notary)
                    .addOutputState(newSavingsState, SmallBankContract.CONTRACT_ID)
                    .addOutputState(newCheckingsState, SmallBankContract.CONTRACT_ID)
                    .addCommand(txCommand);

            if (checkingsState != null)
                txBuilder.addInputState(checkingsStateAndRef);
            if (savingsState != null)
                txBuilder.addInputState(savingsStateAndRef);

            return txBuilder;
        }

        /*
         * Input states: 0 (we do not consume the checked state)
         * Output states: should be one
         */
        @Deprecated
        private TransactionBuilder getBalance(int accountId, Party me, Party notary) {
            SmallBankState savingsState = null;
            SmallBankState checkingsState = null;

            StateAndRef<SmallBankState> savingsStateAndRef = getState(SmallBankState.SavingsTab, accountId);
            if (savingsStateAndRef != null && savingsStateAndRef.getState() != null) {
                savingsState = savingsStateAndRef.getState().getData();
            }
            if (savingsState == null) {
                savingsState = new SmallBankState(me, SmallBankState.SavingsTab, accountId, SmallBankState.DEFAULT_BALANCE, new UniqueIdentifier());
            }
            StateAndRef<SmallBankState> checkingsStateAndRef = getState(SmallBankState.CheckingsTab, accountId);
            if (checkingsStateAndRef != null && checkingsStateAndRef.getState() != null) {
                checkingsState = checkingsStateAndRef.getState().getData();
            }
            if (checkingsState == null) {
                checkingsState = new SmallBankState(me, SmallBankState.CheckingsTab, accountId, SmallBankState.DEFAULT_BALANCE, new UniqueIdentifier());
            }

            Command<CommandData> txCommand = new Command<>(
                    new SmallBankContract.Commands.GetBalance(),
                    ImmutableList.of(me.getOwningKey(), otherParty.getOwningKey()));

            return new TransactionBuilder(notary)
                    .addCommand(txCommand);
        }

        /*
         * Input state: 1 or 0
         * Output state: 1
         */
        private TransactionBuilder updateBalance(int checkingsAccountId, int amount, Party me, Party notary) {
            SmallBankState checkingsState = null;
            SmallBankState newCheckingsState = null;

            Command<CommandData> txCommand = new Command<>(
                    new SmallBankContract.Commands.UpdateBalance(),
                    ImmutableList.of(me.getOwningKey(), otherParty.getOwningKey()));

            TransactionBuilder txBuilder = new TransactionBuilder(notary).addCommand(txCommand);

            StateAndRef<SmallBankState> checkingsStateAndRef = getState(SmallBankState.CheckingsTab, checkingsAccountId);
            if (checkingsStateAndRef != null && checkingsStateAndRef.getState() != null) {
                checkingsState = checkingsStateAndRef.getState().getData();
            }
            if (checkingsState == null) {
                newCheckingsState = new SmallBankState(me, SmallBankState.CheckingsTab, checkingsAccountId, SmallBankState.DEFAULT_BALANCE, new UniqueIdentifier());
            } else {
                newCheckingsState = SmallBankState.copyFromAdjustBalance(checkingsState, amount);
                txBuilder.addInputState(checkingsStateAndRef);
            }

            txBuilder.addOutputState(newCheckingsState, SmallBankContract.CONTRACT_ID);

            return txBuilder;
        }

        /*
         * Input state: 1
         * Output state: 1
         */
        private TransactionBuilder updateSaving(int savingsAccountId, int amount, Party me, Party notary) {
            SmallBankState savingsState = null;
            SmallBankState newSavingsState = null;

            Command<CommandData> txCommand = new Command<>(
                    new SmallBankContract.Commands.UpdateSaving(),
                    ImmutableList.of(me.getOwningKey(), otherParty.getOwningKey()));

            TransactionBuilder txBuilder = new TransactionBuilder(notary).addCommand(txCommand);

            StateAndRef<SmallBankState> savingsStateAndRef = getState(SmallBankState.SavingsTab, savingsAccountId);
            if (savingsStateAndRef != null && savingsStateAndRef.getState() != null) {
                savingsState = savingsStateAndRef.getState().getData();
            }
            if (savingsState == null) {
                newSavingsState = new SmallBankState(me, SmallBankState.SavingsTab, savingsAccountId, SmallBankState.DEFAULT_BALANCE, new UniqueIdentifier());
            } else {
                newSavingsState = SmallBankState.copyFromAdjustBalance(savingsState, amount);
                txBuilder.addInputState(savingsStateAndRef);
            }
            txBuilder.addOutputState(newSavingsState, SmallBankContract.CONTRACT_ID);

            return txBuilder;
        }

        /*
         * Input state: 2, 1, 0
         * Output state: 2
         */
        private TransactionBuilder sendPayment(int fromCheckingsAccountId, int toCheckingsAccountId, int amount, Party me, Party notary) {
            SmallBankState fromCheckingsState = null;
            SmallBankState toCheckingsState = null;
            SmallBankState newFromCheckingsState = null;
            SmallBankState newToCheckingsState = null;

            Command<CommandData> txCommand = new Command<>(
                    new SmallBankContract.Commands.SendPayment(),
                    ImmutableList.of(me.getOwningKey(), otherParty.getOwningKey()));

            TransactionBuilder txBuilder = new TransactionBuilder(notary).addCommand(txCommand);

            StateAndRef<SmallBankState> fromCheckingsStateAndRef = getState(SmallBankState.CheckingsTab, fromCheckingsAccountId);
            if (fromCheckingsStateAndRef != null && fromCheckingsStateAndRef.getState() != null) {
                fromCheckingsState = fromCheckingsStateAndRef.getState().getData();
            }
            if (fromCheckingsState == null) {
                newFromCheckingsState = new SmallBankState(me, SmallBankState.SavingsTab, fromCheckingsAccountId, SmallBankState.DEFAULT_BALANCE - amount, new UniqueIdentifier());
            } else {
                newFromCheckingsState = SmallBankState.copyFromAdjustBalance(fromCheckingsState, -amount);
                txBuilder.addInputState(fromCheckingsStateAndRef);
            }
            StateAndRef<SmallBankState> toCheckingsStateAndRef = getState(SmallBankState.CheckingsTab, toCheckingsAccountId);
            if (toCheckingsStateAndRef != null && toCheckingsStateAndRef.getState() != null) {
                toCheckingsState = toCheckingsStateAndRef.getState().getData();
            }
            if (toCheckingsState == null) {
                newToCheckingsState = new SmallBankState(me, SmallBankState.CheckingsTab, toCheckingsAccountId, SmallBankState.DEFAULT_BALANCE + amount, new UniqueIdentifier());
            } else {
                newToCheckingsState = SmallBankState.copyFromAdjustBalance(toCheckingsState, amount);
                txBuilder.addInputState(toCheckingsStateAndRef);
            }

            txBuilder.addOutputState(newFromCheckingsState, SmallBankContract.CONTRACT_ID)
                    .addOutputState(newToCheckingsState, SmallBankContract.CONTRACT_ID);

            return txBuilder;
        }

        /*
         * Input state: 1, 0
         * Output state: 1
         */
        private TransactionBuilder writeCheck(int accountId, int amount, Party me, Party notary) {
            SmallBankState savingsState = null;
            SmallBankState checkingsState = null;
            SmallBankState newCheckingsState = null;

            Command<CommandData> txCommand = new Command<>(
                    new SmallBankContract.Commands.WriteCheck(),
                    ImmutableList.of(me.getOwningKey(), otherParty.getOwningKey()));

            TransactionBuilder txBuilder = new TransactionBuilder(notary).addCommand(txCommand);

            StateAndRef<SmallBankState> savingsStateAndRef = getState(SmallBankState.SavingsTab, accountId);
            if (savingsStateAndRef != null && savingsStateAndRef.getState() != null) {
                savingsState = savingsStateAndRef.getState().getData();
            }
            if (savingsState == null) {
                savingsState = new SmallBankState(me, SmallBankState.SavingsTab, accountId, SmallBankState.DEFAULT_BALANCE, new UniqueIdentifier());
            }
            StateAndRef<SmallBankState> checkingsStateAndRef = getState(SmallBankState.CheckingsTab, accountId);
            if (checkingsStateAndRef != null && checkingsStateAndRef.getState() != null) {
                checkingsState = checkingsStateAndRef.getState().getData();
            }
            if (checkingsState == null) {
                newCheckingsState = new SmallBankState(me, SmallBankState.CheckingsTab, accountId, SmallBankState.DEFAULT_BALANCE, new UniqueIdentifier());
            } else {
                if (amount > (savingsState.getBalance() + checkingsState.getBalance())) {
                    newCheckingsState = SmallBankState.copyFromAdjustBalance(checkingsState, -amount - 1);

                } else {
                    newCheckingsState = SmallBankState.copyFromAdjustBalance(checkingsState, -amount);
                }
                txBuilder.addInputState(checkingsStateAndRef);
            }

            txBuilder.addOutputState(newCheckingsState, SmallBankContract.CONTRACT_ID);

            return txBuilder;
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

            SmallBankState smallBankState = null;
            StateAndRef<SmallBankState> sorterStateAndRef = null;
            Command<CommandData> txCommand;
            final TransactionBuilder txBuilder;

            switch (contractCommandId) {
                case SmallBankContract.Commands.Amalgate.CMD_ID:
                    txBuilder = amalgate(account1, account2, me, notary);
                    break;
                case SmallBankContract.Commands.UpdateBalance.CMD_ID:
                    txBuilder = updateBalance(account1, amount, me, notary);
                    break;
                case SmallBankContract.Commands.UpdateSaving.CMD_ID:
                    txBuilder = updateSaving(account1, amount, me, notary);
                    break;
                case SmallBankContract.Commands.SendPayment.CMD_ID:
                    txBuilder = sendPayment(account1, account2, amount, me, notary);
                    break;
                case SmallBankContract.Commands.WriteCheck.CMD_ID:
                    txBuilder = writeCheck(account1, amount, me, notary);
                    break;
                default:
                    throw new FlowException("Invalid contract command: " + contractCommandId);
            }

            logger.info("Transaction created.");

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
                        require.using("This must be a Sorter transaction.", output instanceof SmallBankState);
                        return null;
                    });
                }
            }

            return subFlow(new SignTxFlow(otherPartyFlow, SignTransactionFlow.Companion.tracker()));
        }
    }
}
