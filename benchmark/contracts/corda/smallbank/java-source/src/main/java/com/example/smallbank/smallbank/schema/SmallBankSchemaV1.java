package com.example.smallbank.schema;

import com.google.common.collect.ImmutableList;
import net.corda.core.schemas.MappedSchema;
import net.corda.core.schemas.PersistentState;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

public class SmallBankSchemaV1 extends MappedSchema {
    public SmallBankSchemaV1() {
        super(SmallBankSchema.class, 1, ImmutableList.of(PersistentSmallBankState.class));
    }

    @Entity
    @Table(name = "sorter_states")
    public static class PersistentSmallBankState extends PersistentState {
        @Column(name = "owner")
        private final String owner;
        @Column(name = "tab")
        private final int tab;
        @Column(name = "accountId")
        private final int accountId;
        @Column(name = "balance")
        private final int balance;
        @Column(name = "linearId")
        private final String linearId;

        public PersistentSmallBankState(String owner, int tab, int accountId, int balance, String linearId) {
            this.owner = owner;
            this.tab = tab;
            this.accountId = accountId;
            this.balance = balance;
            this.linearId = linearId;
        }

        // Default constructor required by hibernate.
        public PersistentSmallBankState() {
            this.owner = null;
            this.tab = 0;
            this.accountId = 0;
            this.balance = 0;
            this.linearId = null;
        }

        public String getOwner() {
            return owner;
        }

        public int getTab() {
            return tab;
        }

        public int getAccountId() {
            return accountId;
        }

        public int getBalance() {
            return balance;
        }

        public String getId() {
            return linearId;
        }
    }
}