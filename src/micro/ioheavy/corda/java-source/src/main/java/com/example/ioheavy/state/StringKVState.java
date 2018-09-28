package com.example.ioheavy.state;

import com.example.ioheavy.schema.StringKVSchemaV1;
import com.example.kv.KVState;
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

public class StringKVState extends KVState<String,String> implements LinearState, QueryableState {

    static private final Logger logger = LoggerFactory.getLogger(StringKVState.class);

    /*
    public StringKVState() {
        super();
    }

    public StringKVState(String key, String value, List<Party> parties) {
        super(key, value, parties);
    }
    */

    public StringKVState(String key, String value, List<Party> parties, UniqueIdentifier linearId) {
        super(key, value, parties, linearId);
    }

    public static StringKVState copyFrom(StringKVState obj) {
        StringKVState sorterState = new StringKVState(
                obj.getKey(),
                obj.getValue(),
                obj.getParties(),
                new UniqueIdentifier());
        return sorterState;
    }

    @Override
    public UniqueIdentifier getLinearId() {
        return super.getLinearId();
    }

    @Override
    public List<AbstractParty> getParticipants() {
        List<Party> parties = super.getParties();
        ArrayList<AbstractParty> abstractParties = new ArrayList<>(parties.size());
        for (Party p : parties)
            abstractParties.add(p);
        return abstractParties;
    }

    @Override
    public PersistentState generateMappedObject(MappedSchema schema) {
        if (schema instanceof StringKVSchemaV1) {
            return new StringKVSchemaV1.PersistentStringKVState(
                    this.getKey(),
                    this.getValue(),
                    this.getParticipantsName(),
                    this.getLinearId().getId().toString());
        } else {
            throw new IllegalArgumentException("Unrecognised schema $schema");
        }
    }

    @Override
    public Iterable<MappedSchema> supportedSchemas() {
        return ImmutableList.of(new StringKVSchemaV1());
    }

    @Override
    public String toString() {
        return String.format("StringKVState(key=%s, value=%s, linearId=%s)",
                super.getKey(), super.getValue(), super.getLinearId());
    }
}
