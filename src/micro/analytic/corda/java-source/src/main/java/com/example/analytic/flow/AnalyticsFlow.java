package com.example.analytic.flow;

import co.paralleluniverse.fibers.Suspendable;
import com.example.analytic.contract.AnalyticsContract;
import com.example.analytic.schema.BankAccountSchemaV1;
import com.example.analytic.schema.BankTransactionSchemaV1;
import com.example.analytic.state.BankAccount;
import com.example.analytic.state.BankTransaction;
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
public class AnalyticsFlow {

    static private final Logger logger = LoggerFactory.getLogger(AnalyticsFlow.class);

    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<SignedTransaction> {

        private static final int DEFAULT_BALANCE = 1000000000;

        private final int num;
        private final int fromAcc;
        private final int toAcc;
        private final int val;
        private final int contractCommandId;
        private final String txId;
        private final Party otherParty;

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

        public Initiator(int contractCommandId, Party otherParty) {
            this.num = -1;
            this.fromAcc = -1;
            this.toAcc = -1;
            this.val = 0;
            this.txId = null;
            this.contractCommandId = contractCommandId;
            this.otherParty = otherParty;
        }

        public Initiator(int num, int contractCommandId, Party otherParty) {
            this.num = num;
            this.fromAcc = -1;
            this.toAcc = -1;
            this.val = 0;
            this.txId = null;
            this.contractCommandId = contractCommandId;
            this.otherParty = otherParty;
        }

        public Initiator(int fromAcc, int toAcc, int val, int contractCommandId, Party otherParty) {
            this.num = 0;
            this.fromAcc = fromAcc;
            this.toAcc = toAcc;
            this.val = val;
            this.txId = null;
            this.contractCommandId = contractCommandId;
            this.otherParty = otherParty;
        }

        public Initiator(String txId, int contractCommandId, Party otherParty) {
            this.num = -1;
            this.fromAcc = -1;
            this.toAcc = -1;
            this.val = 0;
            this.txId = txId;
            this.contractCommandId = contractCommandId;
            this.otherParty = otherParty;
        }

        @Override
        public ProgressTracker getProgressTracker() {
            return progressTracker;
        }

        private String genAccountId(int k) {
            return String.format("%020d", k);
        }

        private StateAndRef<BankAccount> getAccount(String accountId) {
            try {
                Field key = BankAccountSchemaV1.PersistentBankAccount.class.getDeclaredField("accountId");
                CriteriaExpression index = Builder.equal(key, accountId);
                QueryCriteria myCriteria = new QueryCriteria.VaultCustomQueryCriteria(index);
                return getServiceHub().getVaultService().queryBy(BankAccount.class, myCriteria).getStates().get(0);
            } catch (NoSuchFieldException e1) {
                logger.warn(e1.getMessage());
            } catch (IndexOutOfBoundsException e2) {
                logger.warn(e2.getMessage());
            } catch (Exception e3) {
                logger.warn(e3.getMessage());
            }
            return null;
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

            switch (contractCommandId) {
                case AnalyticsContract.Commands.Init.CMD_ID:
                    if (num < 1)
                        throw new FlowException("Invalid number of accounts to init. Must be >= 1.");

                    txCommand = new Command<>(
                            new AnalyticsContract.Commands.Init(num),
                            ImmutableList.of(me.getOwningKey(), otherParty.getOwningKey()));
                    txBuilder = new TransactionBuilder(notary).addCommand(txCommand);
                    for (int i = 0; i < num; i++) {
                        BankAccount account = new BankAccount(genAccountId(i), 0, DEFAULT_BALANCE, participants, new UniqueIdentifier());
                        txBuilder.addOutputState(account, AnalyticsContract.CONTRACT_ID);
                    }
                    break;
                case AnalyticsContract.Commands.Create.CMD_ID:
                    if (num < 0)
                        throw new FlowException("Invalid account id. Must be >= 0.");

                    txCommand = new Command<>(
                            new AnalyticsContract.Commands.Create(),
                            ImmutableList.of(me.getOwningKey(), otherParty.getOwningKey()));

                    BankAccount account = new BankAccount(genAccountId(num), 0, DEFAULT_BALANCE, participants, new UniqueIdentifier());

                    txBuilder = new TransactionBuilder(notary).addCommand(txCommand).addOutputState(account, AnalyticsContract.CONTRACT_ID);
                    break;
                case AnalyticsContract.Commands.Send.CMD_ID:
                    if (val < 1)
                        throw new FlowException("Invalid transaction value. Must be >= 1.");

                    BankTransaction btx = new BankTransaction(genAccountId(fromAcc), genAccountId(toAcc), val, participants, new UniqueIdentifier());
                    logger.info("Adding bank tx: " + btx.getLinearId().getId().toString());

                    txCommand = new Command<>(
                            new AnalyticsContract.Commands.Send(),
                            ImmutableList.of(me.getOwningKey(), otherParty.getOwningKey()));
                    txBuilder = new TransactionBuilder(notary).addCommand(txCommand)
                            .addOutputState(btx, AnalyticsContract.CONTRACT_ID);
                    break;
                case AnalyticsContract.Commands.Commit.CMD_ID:
                    txCommand = new Command<>(
                            new AnalyticsContract.Commands.Commit(),
                            ImmutableList.of(me.getOwningKey(), otherParty.getOwningKey()));
                    txBuilder = new TransactionBuilder(notary).addCommand(txCommand);

                    StateAndRef<BankTransaction> transactionStateAndRef = null;
                    try {
                        Field linearId = BankTransactionSchemaV1.PersistentBankTransaction.class.getDeclaredField("linearId");
                        CriteriaExpression index = Builder.equal(linearId, txId);
                        QueryCriteria myCriteria = new QueryCriteria.VaultCustomQueryCriteria(index);
                        transactionStateAndRef = getServiceHub().getVaultService().queryBy(BankTransaction.class, myCriteria).getStates().get(0);
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }

                    if (transactionStateAndRef == null)
                        throw new FlowException("Bank transaction not found: " + txId);

                    logger.info("Processing tx: " + transactionStateAndRef.getState().getData().getLinearId().getId().toString());

                    BankTransaction transaction = transactionStateAndRef.getState().getData();
                    StateAndRef<BankAccount> fromAccountStateAndRef = getAccount(transaction.getFrom());
                    StateAndRef<BankAccount> toAccountStateAndRef = getAccount(transaction.getTo());

                    if (fromAccountStateAndRef == null)
                        throw new FlowException("Account not found: " + fromAcc);
                    if (toAccountStateAndRef == null)
                        throw new FlowException("Account not found: " + toAcc);

                    BankAccount fromAccount = fromAccountStateAndRef.getState().getData();
                    BankAccount toAccount = toAccountStateAndRef.getState().getData();
                    BankAccount newFromAccount = BankAccount.copyFromAdjustBalance(fromAccount, -transaction.getVal());
                    BankAccount newToAccount = BankAccount.copyFromAdjustBalance(toAccount, transaction.getVal());

                    txBuilder
                            .addInputState(transactionStateAndRef)
                            .addInputState(fromAccountStateAndRef)
                            .addInputState(toAccountStateAndRef)
                            .addOutputState(newFromAccount, AnalyticsContract.CONTRACT_ID)
                            .addOutputState(newToAccount, AnalyticsContract.CONTRACT_ID);

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

                }
            }

            return subFlow(new SignTxFlow(otherPartyFlow, SignTransactionFlow.Companion.tracker()));
        }
    }
}
