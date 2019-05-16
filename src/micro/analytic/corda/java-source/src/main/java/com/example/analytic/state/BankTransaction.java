package com.example.analytic.state;

import com.example.analytic.schema.BankTransactionSchemaV1;
import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.schemas.MappedSchema;
import net.corda.core.schemas.PersistentState;
import net.corda.core.schemas.QueryableState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BankTransaction implements LinearState, QueryableState {

    static private final Logger logger = LoggerFactory.getLogger(BankTransaction.class);

    private final String from;
    private final String to;
    private final int val;
    private final List<Party> parties;
    private final UniqueIdentifier linearId;

    public BankTransaction(String from, String to, int val, List<Party> parties, UniqueIdentifier linearId) {
        this.from = from;
        this.to = to;
        this.val = val;
        this.parties = parties;
        this.linearId = linearId;
    }

    public static BankTransaction copyFrom(BankTransaction obj) {
        return new BankTransaction(
                obj.getFrom(),
                obj.getTo(),
                obj.getVal(),
                obj.getParties(),
                new UniqueIdentifier());
    }

    public String getFrom() { return from; }

    public String getTo() { return to; }

    public int getVal() { return val; }

    public List<Party> getParties() { return parties; }

    @Override
    public UniqueIdentifier getLinearId() {
        return linearId;
    }

    @Override
    public List<AbstractParty> getParticipants() {
        ArrayList<AbstractParty> abstractParties = new ArrayList<>(parties.size());
        for (Party p : parties)
            abstractParties.add(p);
        return abstractParties;
    }

    public List<String> getParticipantsName() {
        return parties.stream().map(party -> party.getName().toString()).collect(Collectors.toList());
    }

    @Override
    public PersistentState generateMappedObject(MappedSchema schema) {
        if (schema instanceof BankTransactionSchemaV1) {
            return new BankTransactionSchemaV1.PersistentBankTransaction(
                    from,
                    to,
                    val,
                    this.getParticipantsName(),
                    this.getLinearId().getId().toString());
        } else {
            throw new IllegalArgumentException("Unrecognised schema $schema");
        }
    }

    @Override
    public Iterable<MappedSchema> supportedSchemas() {
        return ImmutableList.of(new BankTransactionSchemaV1());
    }

    @Override
    public String toString() {
        return String.format("BankTransaction(from=%s, to=%s, value=%s, linearId=%s)",
                from, to, val, linearId);
    }
}
