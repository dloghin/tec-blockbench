package com.example.ioheavy.contract;

import com.example.ioheavy.state.StringKVState;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.identity.AbstractParty;
import net.corda.core.transactions.LedgerTransaction;

import java.util.stream.Collectors;

import static net.corda.core.contracts.ContractsDSL.requireThat;

public class IOHeavyContract implements Contract {

    public static final String CONTRACT_ID = IOHeavyContract.class.getCanonicalName();

    /**
     * The verify() function of all the states' contracts must not throw an exception for a transaction to be
     * considered valid.
     */
    @Override
    public void verify(LedgerTransaction tx) {
        boolean validCommand = false;
        int nOutputStates = 0;
        if (tx.getCommand(0).getValue() instanceof Commands.Write) {
            validCommand = true;
            nOutputStates = ((Commands.Write)tx.getCommand(0).getValue()).getNKeys();
        } else if (tx.getCommand(0).getValue() instanceof Commands.Scan) {
            validCommand = true;
            nOutputStates = 1;
        }
        final boolean isValidCommand = validCommand;
        final int outputStateCount = nOutputStates;

        requireThat(require -> {
            require.using("Invalid command.", isValidCommand);
            require.using("Input states count not matching.",
                    tx.getInputs().size() == 0);
            require.using("Output states count not matching..",
                    tx.getOutputs().size() == outputStateCount);
            final StringKVState out = tx.outputsOfType(StringKVState.class).get(0);
            require.using("All of the participants must be signers.",
                    tx.getCommand(0).getSigners().containsAll(out.getParties().stream()
                            .map(AbstractParty::getOwningKey)
                            .collect(Collectors.toList())));

            return null;
        });
    }

    public interface Commands extends CommandData {
        class Write implements CommandData {
            public final static int CMD_ID = 1;

            private int nKeys;

            public Write(int nKeys) {
                this.nKeys = nKeys;
            }

            public int getNKeys() { return nKeys; }
        }

        class Scan implements CommandData {
            public final static int CMD_ID = 2;
        }

    }
}
