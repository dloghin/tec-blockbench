package com.example.kvstore.contract;

import com.example.kvstore.state.StringKVState;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.identity.AbstractParty;
import net.corda.core.transactions.LedgerTransaction;

import java.util.stream.Collectors;

import static net.corda.core.contracts.ContractsDSL.requireThat;

public class KVStoreContract implements Contract {

    public static final String CONTRACT_ID = KVStoreContract.class.getCanonicalName();

    /**
     * The verify() function of all the states' contracts must not throw an exception for a transaction to be
     * considered valid.
     */
    @Override
    public void verify(LedgerTransaction tx) {
        boolean validCommand = false;
        int nInputStates = 0;
        int nOutputStates = 0;
        StringKVState state = null;
        if (tx.getCommand(0).getValue() instanceof Commands.Write) {
            validCommand = true;
            nInputStates = 0;
            nOutputStates = 1;
            state = tx.outputsOfType(StringKVState.class).get(0);
        } else if (tx.getCommand(0).getValue() instanceof Commands.Update) {
            validCommand = true;
            nInputStates = 1;
            nOutputStates = 1;
            state = tx.outputsOfType(StringKVState.class).get(0);
        } else if (tx.getCommand(0).getValue() instanceof Commands.Delete) {
            validCommand = true;
            nInputStates = 1;
            nOutputStates = 0;
            state = tx.inputsOfType(StringKVState.class).get(0);
        }
        final boolean isValidCommand = validCommand;
        final int inputStatesCount = nInputStates;
        final int outputStatesCount = nOutputStates;
        final StringKVState xstate = state;

        requireThat(require -> {
            require.using("Invalid command.", isValidCommand);
            require.using("Input states count not matching.",
                    tx.getInputs().size() == inputStatesCount);
            require.using("Output states count not matching..",
                    tx.getOutputs().size() == outputStatesCount);

            require.using("All of the participants must be signers.",
                    tx.getCommand(0).getSigners().containsAll(xstate.getParties().stream()
                            .map(AbstractParty::getOwningKey)
                            .collect(Collectors.toList()))
            );

            return null;
        });
    }

    public interface Commands extends CommandData {
        class Write implements CommandData {
            public final static int CMD_ID = 1;
        }

        class Delete implements CommandData {
            public final static int CMD_ID = 2;
        }

        class Update implements CommandData {
            public final static int CMD_ID = 3;
        }

        /* Read is not an actual command */
        class Read implements CommandData {
            public final static int CMD_ID = 4;
        }
    }
}
