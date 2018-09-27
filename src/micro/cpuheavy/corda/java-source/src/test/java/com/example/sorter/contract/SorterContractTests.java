package com.example.sorter.contract;

import com.example.sorter.state.SorterState;
import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.CordaX500Name;
import net.corda.testing.core.TestIdentity;
import net.corda.testing.node.MockServices;
import org.junit.Test;

import static net.corda.testing.node.NodeTestUtils.ledger;

public class SorterContractTests {
    static private final MockServices ledgerServices = new MockServices();
    static private TestIdentity megaCorp = new TestIdentity(new CordaX500Name("MegaCorp", "London", "GB"));
    static private TestIdentity miniCorp = new TestIdentity(new CordaX500Name("MiniCorp", "London", "GB"));

    @Test
    public void transactionMustIncludeCreateCommand() {
        Integer size = 1000;
        SorterState state = new SorterState(size, null, miniCorp.getParty(), megaCorp.getParty(), new UniqueIdentifier());
        state.genReverseValues();
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.output(SorterContract.CONTRACT_ID, state);
                tx.fails();
                tx.command(ImmutableList.of(megaCorp.getPublicKey(), miniCorp.getPublicKey()), new SorterContract.Commands.Create());
                tx.verifies();
                return null;
            });
            return null;
        }));
    }

    @Test
    public void transactionMustHaveNoInputs() {
        Integer size = 1000;
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.input(SorterContract.CONTRACT_ID, new SorterState(size, null, miniCorp.getParty(), megaCorp.getParty(), new UniqueIdentifier()));
                tx.output(SorterContract.CONTRACT_ID, new SorterState(size, null, miniCorp.getParty(), megaCorp.getParty(), new UniqueIdentifier()));
                tx.command(ImmutableList.of(megaCorp.getPublicKey(), miniCorp.getPublicKey()), new SorterContract.Commands.Create());
                tx.failsWith("Input states count not matching.");
                return null;
            });
            return null;
        }));
    }

    @Test
    public void transactionMustHaveOneOutput() {
        Integer size = 1000;
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.output(SorterContract.CONTRACT_ID, new SorterState(size, null, miniCorp.getParty(), megaCorp.getParty(), new UniqueIdentifier()));
                tx.output(SorterContract.CONTRACT_ID, new SorterState(size, null, miniCorp.getParty(), megaCorp.getParty(), new UniqueIdentifier()));
                tx.command(ImmutableList.of(megaCorp.getPublicKey(), miniCorp.getPublicKey()), new SorterContract.Commands.Create());
                tx.failsWith("Only one output state should be created.");
                return null;
            });
            return null;
        }));
    }

    @Test
    public void contracteeMustSignTransaction() {
        Integer size = 1000;
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.output(SorterContract.CONTRACT_ID, new SorterState(size, null, miniCorp.getParty(), megaCorp.getParty(), new UniqueIdentifier()));
                tx.command(miniCorp.getPublicKey(), new SorterContract.Commands.Create());
                tx.failsWith("All of the participants must be signers.");
                return null;
            });
            return null;
        }));
    }

    @Test
    public void contractorMustSignTransaction() {
        Integer size = 1000;
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.output(SorterContract.CONTRACT_ID, new SorterState(size, null, miniCorp.getParty(), megaCorp.getParty(), new UniqueIdentifier()));
                tx.command(megaCorp.getPublicKey(), new SorterContract.Commands.Create());
                tx.failsWith("All of the participants must be signers.");
                return null;
            });
            return null;
        }));
    }

    @Test
    public void contracteeIsNotContractor() {
        Integer size = 1000;
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.output(SorterContract.CONTRACT_ID, new SorterState(size, null, megaCorp.getParty(), megaCorp.getParty(), new UniqueIdentifier()));
                tx.command(ImmutableList.of(megaCorp.getPublicKey(), miniCorp.getPublicKey()), new SorterContract.Commands.Create());
                tx.failsWith("The contractor and the contractee cannot be the same entity.");
                return null;
            });
            return null;
        }));
    }

    @Test
    public void cannotCreateNegativeValueSize() {
        Integer size = -1;
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.output(SorterContract.CONTRACT_ID, new SorterState(size, null, miniCorp.getParty(), megaCorp.getParty(), new UniqueIdentifier()));
                tx.command(ImmutableList.of(megaCorp.getPublicKey(), miniCorp.getPublicKey()), new SorterContract.Commands.Create());
                tx.failsWith("Invalid list size.");
                return null;
            });
            return null;
        }));
    }
}