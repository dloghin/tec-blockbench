package com.example.sorter.state;

import com.example.sorter.schema.SorterSchemaV1;
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
import java.util.Collections;
import java.util.List;

public class SorterState implements LinearState, QueryableState {

    private final UniqueIdentifier linearId;
    private int size;
    private List<Integer> values;
    private final Party contractee;
    private final Party contractor;

    static private final Logger logger = LoggerFactory.getLogger(SorterState.class);

    public SorterState(int size, List<Integer> values, Party contractee, Party contractor, UniqueIdentifier linearId) {
        this.size = size;
        this.values = values;
        this.contractee = contractee;
        this.contractor = contractor;
        this.linearId = linearId;
        logger.debug("Constructor called for state: " + linearId.getId().toString());
    }

    /**
     * Initialize list with reverse-ordered values.
     */
    public void genReverseValues() {
        values = new ArrayList<Integer>(size);
        for (int i = 0; i < size; i++)
            values.add(size - i);
    }

    /**
     * Copy from another state object.
     *
     * @param obj
     * @return
     */
    public static SorterState copyFrom(SorterState obj) {
        SorterState sorterState = new SorterState(
                obj.getSize(),
                new ArrayList<Integer>(obj.getSize()),
                obj.getContractee(),
                obj.getContractor(),
                new UniqueIdentifier());
        for (int i = 0; i < obj.getSize(); i++)
            sorterState.setValue(i, obj.getValues().get(i));
        return sorterState;
    }

    public int getSize() {
        return size;
    }

    public List<Integer> getValues() {
        return values;
    }

    public Party getContractee() {
        return contractee;
    }

    public Party getContractor() {
        return contractor;
    }

    public void setValue(int index, Integer value) {
        values.add(index, value);
    }

    /**
     * Check if values are sorted in ascending order. If the list is null, return false by default.
     *
     * @return
     */
    public boolean isSorted() {
        if (values == null)
            return false;
        for (int i = 1; i < values.size(); i++)
            if (values.get(i - 1) > values.get(i))
                return false;
        return true;
    }

    public void sort() {
        Collections.sort(values);
    }

    @Override
    public UniqueIdentifier getLinearId() {
        return linearId;
    }

    @Override
    public List<AbstractParty> getParticipants() {
        return Arrays.asList(contractee, contractor);
    }

    @Override
    public PersistentState generateMappedObject(MappedSchema schema) {
        if (schema instanceof SorterSchemaV1) {
            return new SorterSchemaV1.PersistentSorterState(
                    this.contractee.getName().toString(),
                    this.contractor.getName().toString(),
                    this.size,
                    this.values,
                    this.linearId.getId().toString());
        } else {
            throw new IllegalArgumentException("Unrecognised schema $schema");
        }
    }

    @Override
    public Iterable<MappedSchema> supportedSchemas() {
        return ImmutableList.of(new SorterSchemaV1());
    }

    @Override
    public String toString() {
        return String.format("SorterState(size=%s, contractee=%s, contractor=%s, linearId=%s)",
                size, contractee, contractor, linearId);
    }
}
