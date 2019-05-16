package com.example.analytic.schema;

import com.google.common.collect.ImmutableList;
import net.corda.core.schemas.MappedSchema;
import net.corda.core.schemas.PersistentState;

import javax.persistence.*;
import java.util.List;

public class BankTransactionSchemaV1 extends MappedSchema {

    public BankTransactionSchemaV1() {
        super(BankTransactionSchema.class, 1, ImmutableList.of(PersistentBankTransaction.class));
    }

    @Entity(name = "btx")
    @Table(name = "analytictransactions")
    public static class PersistentBankTransaction extends PersistentState {
        @Column(name = "from")
        private final String from;

        @Column(name = "to")
        private final String to;

        @Column(name = "val")
        private final int val;

        @Column(name = "participants")
        @ElementCollection
        private final List<String> participants;

        @Id
        @Column(name = "linearId")
        private final String linearId;


        public PersistentBankTransaction(
                String from, String to, int val, List<String> participants, String linearId) {
            this.from = from;
            this.to = to;
            this.val = val;
            this.participants = participants;
            this.linearId = linearId;
        }

        // Default constructor required by hibernate.
        public PersistentBankTransaction() {
            this.from = null;
            this.to = null;
            this.val = 0;
            this.participants = null;
            this.linearId = null;
        }

        public String getFrom() { return from; }

        public String getTo() { return to; }

        public int getVal() { return val; }

        public List<String> getParticipants() { return participants; }

        public String getId() { return linearId; }
    }
}
