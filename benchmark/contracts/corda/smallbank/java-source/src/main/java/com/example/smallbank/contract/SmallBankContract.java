package com.example.smallbank.contract;

import com.example.smallbank.state.SmallBankState;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.identity.AbstractParty;
import net.corda.core.transactions.LedgerTransaction;

import java.security.PublicKey;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static net.corda.core.contracts.ContractsDSL.requireThat;

public class SmallBankContract implements Contract {

    public static final String CONTRACT_ID = SmallBankContract.class.getCanonicalName();

    /**
     * The verify() function of all the states' contracts must not throw an exception for a transaction to be
     * considered valid.
     */
    @Override
    public void verify(LedgerTransaction tx) {
        int noOfInputStates = 0;
        int noOfOutputStates = 0;
        boolean validCommand = false;
        if (tx.getCommand(0).getValue() instanceof Commands.Amalgate) {
            noOfInputStates = Commands.Amalgate.N_INPUT_STATES;
            noOfOutputStates = Commands.Amalgate.N_OUTPUT_STATES;
            validCommand = true;
        } else if (tx.getCommand(0).getValue() instanceof Commands.GetBalance) {
            noOfInputStates = Commands.GetBalance.N_INPUT_STATES;
            noOfOutputStates = Commands.GetBalance.N_OUTPUT_STATES;
            validCommand = true;
        } else if (tx.getCommand(0).getValue() instanceof Commands.UpdateBalance) {
            noOfInputStates = Commands.UpdateBalance.N_INPUT_STATES;
            noOfOutputStates = Commands.UpdateBalance.N_OUTPUT_STATES;
            validCommand = true;
        } else if (tx.getCommand(0).getValue() instanceof Commands.UpdateSaving) {
            noOfInputStates = Commands.UpdateSaving.N_INPUT_STATES;
            noOfOutputStates = Commands.UpdateSaving.N_OUTPUT_STATES;
            validCommand = true;
        } else if (tx.getCommand(0).getValue() instanceof Commands.SendPayment) {
            noOfInputStates = Commands.SendPayment.N_INPUT_STATES;
            noOfOutputStates = Commands.SendPayment.N_OUTPUT_STATES;
            validCommand = true;
        } else if (tx.getCommand(0).getValue() instanceof Commands.WriteCheck) {
            noOfInputStates = Commands.WriteCheck.N_INPUT_STATES;
            noOfOutputStates = Commands.WriteCheck.N_OUTPUT_STATES;
            validCommand = true;
        }
        else
            validCommand = false;
        final boolean isValidCommand = validCommand;
        final int countInputStates = noOfInputStates;
        final int countOutputStates = noOfOutputStates;

        requireThat(require -> {
            require.using("Valid command", isValidCommand);
            require.using("Input states count match",
                    tx.getInputs().size() <= countInputStates);
            require.using("Output states count match",
                    tx.getOutputs().size() == countOutputStates);

            List<PublicKey> signers = new LinkedList<>();
            for (int i = 0; i < countOutputStates; i++) {
                final SmallBankState out = tx.outputsOfType(SmallBankState.class).get(i);
                signers.addAll(out.getParticipants().stream()
                        .map(AbstractParty::getOwningKey)
                        .collect(Collectors.toList()));
            }
            require.using("All of the participants must be signers.",
                    tx.getCommand(0).getSigners().containsAll(signers));

            return null;
        });
    }

    public interface Commands extends CommandData {
        class Amalgate implements CommandData {
            public final static int CMD_ID = 1;
            public final static int N_INPUT_STATES = 2;
            public final static int N_OUTPUT_STATES = 2;
        }

        class GetBalance implements CommandData {
            public final static int CMD_ID = 2;
            public final static int N_INPUT_STATES = 0;
            public final static int N_OUTPUT_STATES = 0;
        }

        class UpdateBalance implements CommandData {
            public final static int CMD_ID = 3;
            public final static int N_INPUT_STATES = 1;
            public final static int N_OUTPUT_STATES = 1;
        }

        class UpdateSaving implements CommandData {
            public final static int CMD_ID = 4;
            public final static int N_INPUT_STATES = 1;
            public final static int N_OUTPUT_STATES = 1;
        }

        class SendPayment implements CommandData {
            public final static int CMD_ID = 5;
            public final static int N_INPUT_STATES = 2;
            public final static int N_OUTPUT_STATES = 2;
        }

        class WriteCheck implements CommandData {
            public final static int CMD_ID = 6;
            public final static int N_INPUT_STATES = 1;
            public final static int N_OUTPUT_STATES = 1;
        }
    }
}
