package com.example.analytic.contract;

import com.example.analytic.state.BankAccount;
import com.example.analytic.state.BankTransaction;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.transactions.LedgerTransaction;

import java.util.List;
import java.util.stream.Collectors;

import static net.corda.core.contracts.ContractsDSL.requireThat;

public class AnalyticsContract implements Contract {

    public static final String CONTRACT_ID = AnalyticsContract.class.getCanonicalName();

    /**
     * The verify() function of all the states' contracts must not throw an exception for a transaction to be
     * considered valid.
     */
    @Override
    public void verify(LedgerTransaction tx) {
        boolean validCommand = false;
        int nOutputStates = 0;
        int nInputStates = 0;
        List<Party> parties = null;
        if (tx.getCommand(0).getValue() instanceof Commands.Init) {
            validCommand = true;
            nInputStates = 0;
            nOutputStates = ((Commands.Init) tx.getCommand(0).getValue()).getNumAccounts();
            parties = tx.outputsOfType(BankAccount.class).get(0).getParties();
        } else if (tx.getCommand(0).getValue() instanceof Commands.Create) {
            validCommand = true;
            nInputStates = 0;
            nOutputStates = 1;
            parties = tx.outputsOfType(BankAccount.class).get(0).getParties();
        } else if (tx.getCommand(0).getValue() instanceof Commands.Send) {
            validCommand = true;
            nInputStates = 0;
            nOutputStates = 1;
            parties = tx.outputsOfType(BankTransaction.class).get(0).getParties();
        } else if (tx.getCommand(0).getValue() instanceof Commands.Commit) {
            validCommand = true;
            nInputStates = 3;
            nOutputStates = 2;
            parties = tx.outputsOfType(BankAccount.class).get(0).getParties();
        }
        final boolean isValidCommand = validCommand;
        final int inputStateCount = nInputStates;
        final int outputStateCount = nOutputStates;
        final List<Party> participants = parties;

        requireThat(require -> {
            require.using("Invalid command.", isValidCommand);
            require.using("Input states count not matching.",
                    tx.getInputs().size() == inputStateCount);
            require.using("Output states count not matching..",
                    tx.getOutputs().size() == outputStateCount);
            require.using("All of the participants must be signers.",
                    tx.getCommand(0).getSigners().containsAll(participants.stream()
                            .map(AbstractParty::getOwningKey)
                            .collect(Collectors.toList())));

            return null;
        });
    }

    public interface Commands extends CommandData {
        class Init implements CommandData {
            public final static int CMD_ID = 1;

            private int numAccounts;

            public Init(int numAccounts) {
                this.numAccounts = numAccounts;
            }

            public int getNumAccounts() {
                return numAccounts;
            }
        }


        class Create implements CommandData {
            public final static int CMD_ID = 2;
        }

        class Send implements CommandData {
            public final static int CMD_ID = 3;
        }

        class Commit implements CommandData {
            public final static int CMD_ID = 4;
        }
    }
}
