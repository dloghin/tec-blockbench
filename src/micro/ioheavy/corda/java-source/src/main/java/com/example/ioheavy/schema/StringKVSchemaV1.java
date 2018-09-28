package com.example.ioheavy.schema;

import com.google.common.collect.ImmutableList;
import net.corda.core.schemas.MappedSchema;
import net.corda.core.schemas.PersistentState;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.List;

public class StringKVSchemaV1 extends MappedSchema {

    public StringKVSchemaV1() {
        super(StringKVSchema.class, 1, ImmutableList.of(PersistentStringKVState.class));
    }

    @Entity
    @Table(name = "string_kv_states")
    public static class PersistentStringKVState extends PersistentState {
        @Column(name = "key")
        private final String key;

        @Column(name = "value")
        private final String value;

        @Column(name = "participants")
        @ElementCollection
        private final List<String> participants;

        @Column(name = "linearId")
        private final String linearId;


        public PersistentStringKVState(
                String key, String value, List<String> participants, String linearId) {
            this.key = key;
            this.value = value;
            this.participants = participants;
            this.linearId = linearId;
        }

        // Default constructor required by hibernate.
        public PersistentStringKVState() {
            this.key = null;
            this.value = null;
            this.participants = null;
            this.linearId = null;
        }

        public String getKey() { return key; }

        public String getValue() { return value; }

        public List<String> getParticipants() { return participants; }

        public String getId() { return linearId; }
    }
}
