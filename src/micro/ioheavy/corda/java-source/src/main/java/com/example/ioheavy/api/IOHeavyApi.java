package com.example.ioheavy.api;

import com.example.ioheavy.flow.IOHeavyFlow;
import com.example.ioheavy.contract.IOHeavyContract;
import com.example.ioheavy.state.StringKVState;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.node.NodeInfo;
import net.corda.core.node.services.Vault;
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

// This API is accessible from /api/ioheavy. All paths specified below are relative to it.
@Path("ioheavy")
public class IOHeavyApi {
    private final CordaRPCOps rpcOps;
    private final CordaX500Name myLegalName;

    private final List<String> serviceNames = ImmutableList.of("Notary");

    static private final Logger logger = LoggerFactory.getLogger(IOHeavyApi.class);

    public IOHeavyApi(CordaRPCOps rpcOps) {
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
    public List<StateAndRef<StringKVState>> getIOUs() {
        return rpcOps.vaultQuery(StringKVState.class).getStates();
    }

    /**
     * Initiate create flow.
     */
    @PUT
    @Path("ioheavy")
    public Response ioheavyFlow(
            @QueryParam("startKey") int startKey,
            @QueryParam("numKeys") int numKeys,
            @QueryParam("command") int command,
            @QueryParam("partyName") String partyName)
            throws InterruptedException, ExecutionException {

        logger.info("API: ioheavy, command: " + command + ", party name: " + partyName);

        if (command < IOHeavyContract.Commands.Write.CMD_ID || command > IOHeavyContract.Commands.Scan.CMD_ID) {
            return Response
                    .status(BAD_REQUEST).entity("Query parameter 'command' is invalid.\n").build();
        }

        if (startKey < 0 || numKeys < 1) {
            return Response
                    .status(BAD_REQUEST).entity("Query parameter 'startKey' or 'numKeys' are invalid.\n").build();
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

        try {
            final SignedTransaction signedTx = rpcOps
                    .startTrackedFlowDynamic(
                            IOHeavyFlow.Initiator.class, startKey, numKeys, command, otherParty)
                    .getReturnValue()
                    .get();

            StringKVState sorterState = (StringKVState) signedTx.getTx().getOutput(0);
            final String msg =
                    String.format(
                            "Transaction id %s committed to ledger. State id %s\n",
                            signedTx.getId(),
                            sorterState.getLinearId().toString());
            return Response.status(CREATED).entity(msg).build();

        } catch (Throwable ex) {
            final String msg = ex.getMessage();
            logger.error(ex.getMessage(), ex);
            return Response.status(BAD_REQUEST).entity(msg).build();
        }
    }

}
