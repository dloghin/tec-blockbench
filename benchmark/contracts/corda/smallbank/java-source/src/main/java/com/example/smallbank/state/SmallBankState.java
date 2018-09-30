package com.example.smallbank.state;

import com.example.smallbank.schema.SmallBankSchemaV1;
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
import java.util.Arrays;
import java.util.List;

public class SmallBankState implements LinearState, QueryableState {

    public static final int DEFAULT_BALANCE = 100000;

    public static final int AccountsTab = 1;
    public static final int SavingsTab = 2;
    public static final int CheckingsTab = 3;

    private final UniqueIdentifier linearId;
    private final Party owner;
    private final int accountId;
    private final int balance;
    private final int tab;

    static private final Logger logger = LoggerFactory.getLogger(SmallBankState.class);

    public SmallBankState(Party owner, int tab, int accountId, int balance, UniqueIdentifier linearId) {
        this.owner = owner;
        this.tab = tab;
        this.accountId = accountId;
        this.balance = balance;
        this.linearId = linearId;
    }

    /**
     * Copy from another state object.
     *
     * @param obj
     * @return
     */
    public static SmallBankState scopyFrom(SmallBankState obj) {
        return new SmallBankState(
                obj.getOwner(),
                obj.getTab(),
                obj.getAccountId(),
                obj.getBalance(), new UniqueIdentifier());

    }

    public static SmallBankState copyFromAdjustBalance(SmallBankState obj, int balanceDiff) {
        return new SmallBankState(
                obj.getOwner(),
                obj.getTab(),
                obj.getAccountId(),
                obj.getBalance() + balanceDiff,
                new UniqueIdentifier());
    }

    public Party getOwner() {
        return owner;
    }

    public int getBalance() {
        return balance;
    }

    public int getTab() {
        return tab;
    }

    public int getAccountId() {
        return accountId;
    }

    @Override
    public UniqueIdentifier getLinearId() {
        return linearId;
    }

    @Override
    public List<AbstractParty> getParticipants() {
        return Arrays.asList(owner);
    }

    @Override
    public PersistentState generateMappedObject(MappedSchema schema) {
        if (schema instanceof SmallBankSchemaV1) {
            return new SmallBankSchemaV1.PersistentSmallBankState(
                    this.owner.getName().toString(),
                    this.tab,
                    this.accountId,
                    this.balance,
                    this.linearId.getId().toString());
        } else {
            throw new IllegalArgumentException("Unrecognised schema $schema");
        }
    }

    @Override
    public Iterable<MappedSchema> supportedSchemas() {
        return ImmutableList.of(new SmallBankSchemaV1());
    }

    @Override
    public String toString() {
        return String.format("SmallBankState(state=%s, owner=%s, tab=%i, account=%i, balance=$%i)",
                linearId, owner, tab, accountId, balance);
    }
}
