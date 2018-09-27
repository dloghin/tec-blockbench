package com.example.sorter.contract;

import com.example.sorter.state.SorterState;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.identity.AbstractParty;
import net.corda.core.transactions.LedgerTransaction;

import java.util.stream.Collectors;

import static net.corda.core.contracts.ContractsDSL.requireThat;

public class SorterContract implements Contract {

    public static final String CONTRACT_ID = SorterContract.class.getCanonicalName();

    /**
     * The verify() function of all the states' contracts must not throw an exception for a transaction to be
     * considered valid.
     */
    @Override
    public void verify(LedgerTransaction tx) {
        int noOfInputStates = 0;
        boolean validCommand = false;
        if (tx.getCommand(0).getValue() instanceof Commands.Create) {
            noOfInputStates = 0;
            validCommand = true;
        } else if (tx.getCommand(0).getValue() instanceof Commands.Sort) {
            noOfInputStates = 1;
            validCommand = true;
        } else if (tx.getCommand(0).getValue() instanceof Commands.CreateAndSort) {
            noOfInputStates = 0;
            validCommand = true;
        } else
            validCommand = false;
        final boolean isValidCommand = validCommand;
        final int countInputStates = noOfInputStates;

        requireThat(require -> {
            require.using("Invalid command.", isValidCommand);
            require.using("Input states count not matching.",
                    tx.getInputs().size() == countInputStates);
            require.using("Only one output state should be created.",
                    tx.getOutputs().size() == 1);
            final SorterState out = tx.outputsOfType(SorterState.class).get(0);
            require.using("Invalid list size.", out.getSize() >= 2);
            if (tx.getCommand(0).getValue() instanceof Commands.Create) {
                require.using("Values must be unsorted.", !out.isSorted());
            } else {
                require.using("Values must be sorted.", out.isSorted());
            }
            require.using("The contractor and the contractee cannot be the same entity.",
                    out.getContractee() != out.getContractor());
            require.using("All of the participants must be signers.",
                    tx.getCommand(0).getSigners().containsAll(out.getParticipants().stream()
                            .map(AbstractParty::getOwningKey)
                            .collect(Collectors.toList())));

            return null;
        });
    }

    public interface Commands extends CommandData {
        class Create implements CommandData {
            public final static int CMD_ID = 1;
        }

        class Sort implements CommandData {
            public final static int CMD_ID = 2;
        }

        class CreateAndSort implements CommandData {
            public final static int CMD_ID = 3;
        }
    }
}
