package com.example.analytic.state;

import com.example.analytic.schema.BankAccountSchemaV1;
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

public class BankAccount implements LinearState, QueryableState {

    static private final Logger logger = LoggerFactory.getLogger(BankAccount.class);

    private final String accountId;
    private final int version;
    private final int val;
    private final List<Party> parties;
    private final UniqueIdentifier linearId;

    public BankAccount(String accountId, int version, int val, List<Party> parties, UniqueIdentifier linearId) {
        this.accountId = accountId;
        this.version = version;
        this.val = val;
        this.parties = parties;
        this.linearId = linearId;
    }

    public static BankAccount copyFrom(BankAccount obj) {
        return new BankAccount(
                obj.getAccountId(),
                obj.getVersion() + 1,
                obj.getVal(),
                obj.getParties(),
                new UniqueIdentifier());
    }

    public static BankAccount copyFromAdjustBalance(BankAccount obj, int delta) {
        return new BankAccount(
                obj.getAccountId(),
                obj.getVersion() + 1,
                obj.getVal() + delta,
                obj.getParties(),
                new UniqueIdentifier());
    }

    public String getAccountId() { return accountId; }

    public int getVersion() { return version; }

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
        if (schema instanceof BankAccountSchemaV1) {
            return new BankAccountSchemaV1.PersistentBankAccount(
                    accountId,
                    version,
                    val,
                    this.getParticipantsName(),
                    this.getLinearId().getId().toString());
        } else {
            throw new IllegalArgumentException("Unrecognised schema $schema");
        }
    }

    @Override
    public Iterable<MappedSchema> supportedSchemas() {
        return ImmutableList.of(new BankAccountSchemaV1());
    }

    @Override
    public String toString() {
        return String.format("BankAccount(accountId=%s, version=%s, value=%s, linearId=%s)",
                accountId, version, val, linearId);
    }
}
