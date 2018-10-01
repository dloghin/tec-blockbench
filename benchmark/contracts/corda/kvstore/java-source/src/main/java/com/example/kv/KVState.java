package com.example.kv;

import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.Party;

import java.util.List;
import java.util.stream.Collectors;

public class KVState<K, V> {

    private final UniqueIdentifier linearId;
    private final K key;
    private final V value;
    private final List<Party> parties;

    public KVState(K key, V value) {
        linearId = new UniqueIdentifier();
        this.key = key;
        this.value = value;
        parties = null;
    }

    public KVState(K key, V value, List<Party> participants) {
        linearId = new UniqueIdentifier();
        this.key = key;
        this.value = value;
        this.parties = participants;
    }

    public KVState(K key, V value, List<Party> participants, UniqueIdentifier linearId) {
        this.linearId = linearId;
        this.key = key;
        this.value = value;
        this.parties = participants;
    }

    public KVState() {
        this.linearId = null;
        this.key = null;
        this.value = null;
        this.parties = null;
    }

    public K getKey() {
        return key;
    }

    public V getValue() {
        return value;
    }

    public List<Party> getParties() {
        return parties;
    }

    public UniqueIdentifier getLinearId() {
        return linearId;
    }

    public List<String> getParticipantsName() {
        return parties.stream().map(party -> party.getName().toString()).collect(Collectors.toList());
    }
}
