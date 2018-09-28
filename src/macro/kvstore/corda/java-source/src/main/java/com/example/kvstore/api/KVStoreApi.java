package com.example.kvstore.api;

import com.example.kvstore.flow.KVStoreFlow;
import com.example.kvstore.contract.KVStoreContract;
import com.example.kvstore.schema.StringKVSchemaV1;
import com.example.kvstore.state.StringKVState;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.node.NodeInfo;
import net.corda.core.node.services.vault.Builder;
import net.corda.core.node.services.vault.CriteriaExpression;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.Response.Status.*;

// This API is accessible from /api/kvstore. All paths specified below are relative to it.
@Path("kvstore")
public class KVStoreApi {
    private final CordaRPCOps rpcOps;
    private final CordaX500Name myLegalName;

    private final List<String> serviceNames = ImmutableList.of("Notary");

    static private final Logger logger = LoggerFactory.getLogger(KVStoreApi.class);

    public KVStoreApi(CordaRPCOps rpcOps) {
        this.rpcOps = rpcOps;
        this.myLegalName = rpcOps.nodeInfo().getLegalIdentities().get(0).getName();
    }

    private CordaX500Name getPartyFromSimpleName(String partyName) {
        CordaX500Name party = null;
        List<CordaX500Name> peers = rpcOps.networkMapSnapshot().stream()
                .map(node -> node.getLegalIdentities().get(0).getName())
                .filter(name -> !name.equals(myLegalName) && !serviceNames.contains(name.getOrganisation()))
                .collect(toList());
        for (CordaX500Name peer : peers) {
            String peerName = peer.getOrganisation();
            logger.info("Peer name: " + peerName);
            if (partyName.equals(peerName)) {
                party = peer;
                break;
            }
        }
        return party;
    }

    /**
     * Returns the node's name.
     */
    @GET
    @Path("me")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, CordaX500Name> whoami() {
        return ImmutableMap.of("me", myLegalName);
    }

    /**
     * Returns all parties registered with the [NetworkMapService]. These names can be used to look up identities
     * using the [IdentityService].
     */
    @GET
    @Path("peers")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, List<CordaX500Name>> getPeers() {
        List<NodeInfo> nodeInfoSnapshot = rpcOps.networkMapSnapshot();
        return ImmutableMap.of("peers", nodeInfoSnapshot
                .stream()
                .map(node -> node.getLegalIdentities().get(0).getName())
                .filter(name -> !name.equals(myLegalName) && !serviceNames.contains(name.getOrganisation()))
                .collect(toList()));
    }

    /**
     * Displays all Sorter states that exist in the node's vault.
     */
    @GET
    @Path("states")
    @Produces(MediaType.APPLICATION_JSON)
    public List<StateAndRef<StringKVState>> getStates() {
        return rpcOps.vaultQuery(StringKVState.class).getStates();
    }

    /**
     * Initiate create flow.
     */
    @PUT
    @Path("kvstore")
    public Response kvstoreFlow(
            @QueryParam("key") String key,
            @QueryParam("val") String val,
            @QueryParam("command") int command,
            @QueryParam("partyName") String partyName)
            throws InterruptedException, ExecutionException {

        logger.info("API: kvstore, command: " + command + ", party name: " + partyName);

        if (command < KVStoreContract.Commands.Write.CMD_ID || command > KVStoreContract.Commands.Read.CMD_ID) {
            return Response
                    .status(BAD_REQUEST).entity("Query parameter 'command' is invalid.\n").build();
        }

        if (key == null || key.isEmpty()) {
            return Response
                    .status(BAD_REQUEST).entity("Query parameter 'key' is empty.\n").build();
        }

        if (val == null || val.isEmpty()) {
            if (command == KVStoreContract.Commands.Write.CMD_ID || command == KVStoreContract.Commands.Update.CMD_ID) {
                return Response
                        .status(BAD_REQUEST).entity("Query parameter 'val' is empty.\n").build();
            }
            else
                val = "";
        }

        if (partyName == null) {
            return Response
                    .status(BAD_REQUEST).entity("Query parameter 'partyName' missing or has wrong format.\n").build();
        }

        CordaX500Name party = getPartyFromSimpleName(partyName);
        if (party == null) {
            return Response
                    .status(BAD_REQUEST).entity("Query parameter 'partyName' missing or has wrong format.\n").build();
        }
        final Party otherParty = rpcOps.wellKnownPartyFromX500Name(party);
        if (otherParty == null) {
            return Response
                    .status(BAD_REQUEST).entity("Party named " + partyName + "cannot be found.\n").build();
        }

        if (command == KVStoreContract.Commands.Read.CMD_ID) {
            try {
                Field keyField = StringKVSchemaV1.PersistentStringKVState.class.getDeclaredField("key");
                CriteriaExpression index = Builder.equal(keyField, key);
                QueryCriteria myCriteria = new QueryCriteria.VaultCustomQueryCriteria(index);
                StringKVState state = rpcOps.vaultQueryByCriteria(myCriteria, StringKVState.class).getStates().get(0).getState().getData();
                return Response.status(CREATED).entity("State found: " + state.toJson() + "\n").build();
            } catch (Exception e) {
                return Response
                        .status(BAD_REQUEST).entity("State not found: " + e.getMessage() + "\n").build();
            }
        } else {
            try {
                final SignedTransaction signedTx = rpcOps
                        .startTrackedFlowDynamic(
                                KVStoreFlow.Initiator.class, key, val, command, otherParty)
                        .getReturnValue()
                        .get();

                String stateId = null;
                if (command == KVStoreContract.Commands.Delete.CMD_ID)
                    stateId = signedTx.getTx().getInputs().get(0).getTxhash().toString();
                else
                    stateId = ((StringKVState) signedTx.getTx().getOutput(0)).getLinearId().toString();
                final String msg =
                        String.format(
                                "Transaction id %s committed to ledger. State id %s\n",
                                signedTx.getId(),
                                stateId);
                return Response.status(CREATED).entity(msg).build();

            } catch (Throwable ex) {
                final String msg = ex.getMessage();
                logger.error(ex.getMessage(), ex);
                return Response.status(BAD_REQUEST).entity(msg).build();
            }
        }
    }

}
