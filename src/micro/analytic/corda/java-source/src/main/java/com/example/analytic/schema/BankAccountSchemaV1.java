package com.example.analytic.schema;

import com.google.common.collect.ImmutableList;
import net.corda.core.schemas.MappedSchema;
import net.corda.core.schemas.PersistentState;

import javax.persistence.*;
import java.util.List;

public class BankAccountSchemaV1 extends MappedSchema {

    public BankAccountSchemaV1() {
        super(BankAccountSchema.class, 1, ImmutableList.of(PersistentBankAccount.class));
    }

    @Entity(name = "bacc")
    @Table(name = "analyticaccounts")
    public static class PersistentBankAccount extends PersistentState {
        @Column(name = "accountId")
        private final String accountId;

        @Column(name = "version")
        private final int version;

        @Column(name = "val")
        private final int val;

        @Column(name = "participants")
        @ElementCollection
        private final List<String> participants;

        @Id
        @Column(name = "linearId")
        private final String linearId;


        public PersistentBankAccount(
                String accountId, int version, int val, List<String> participants, String linearId) {
            this.accountId = accountId;
            this.version = version;
            this.val = val;
            this.participants = participants;
            this.linearId = linearId;
        }

        // Default constructor required by hibernate.
        public PersistentBankAccount() {
            this.accountId = null;
            this.version = 0;
            this.val = 0;
            this.participants = null;
            this.linearId = null;
        }

        public String getAccountId() { return accountId; }

        public int getVersion() { return version; }

        public int getVal() { return val; }

        public List<String> getParticipants() { return participants; }

        public String getId() { return linearId; }
    }
}
