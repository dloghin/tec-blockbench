package com.example.sorter.schema;

import com.google.common.collect.ImmutableList;
import net.corda.core.schemas.MappedSchema;
import net.corda.core.schemas.PersistentState;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.List;

public class SorterSchemaV1 extends MappedSchema {
    public SorterSchemaV1() {
        super(SorterSchema.class, 1, ImmutableList.of(PersistentSorterState.class));
    }

    @Entity
    @Table(name = "sorter_states")
    public static class PersistentSorterState extends PersistentState {
        @Column(name = "contractee")
        private final String contractee;
        @Column(name = "contractor")
        private final String contractor;
        @Column(name = "size")
        private final int size;
        @Column(name = "values")
        @ElementCollection
        private final List<Integer> values;
        @Column(name = "linearId")
        private final String linearId;


        public PersistentSorterState(
                String contractee, String contractor, int size, List<Integer> values, String linearId) {
            this.contractee = contractee;
            this.contractor = contractor;
            this.size = size;
            this.values = values;
            this.linearId = linearId;
        }

        // Default constructor required by hibernate.
        public PersistentSorterState() {
            this.contractee = null;
            this.contractor = null;
            this.size = 0;
            this.values = null;
            this.linearId = null;
        }

        public String getContractee() {
            return contractee;
        }

        public String getContractor() {
            return contractor;
        }

        public int getSize() {
            return size;
        }

        public List<Integer> getValues() {
            return values;
        }

        public String getId() {
            return linearId;
        }
    }
}