package com.example.sorter.api;

import com.example.sorter.flow.SorterFlow;
import com.example.sorter.schema.SorterSchemaV1;
import com.example.sorter.contract.SorterContract;
import com.example.sorter.state.SorterState;
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

// This API is accessible from /api/sorter. All paths specified below are relative to it.
@Path("sorter")
public class SorterApi {
    private final CordaRPCOps rpcOps;
    private final CordaX500Name myLegalName;

    private final List<String> serviceNames = ImmutableList.of("Notary");

    static private final Logger logger = LoggerFactory.getLogger(SorterApi.class);

    public SorterApi(CordaRPCOps rpcOps) {
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
    @Path("sorters")
    @Produces(MediaType.APPLICATION_JSON)
    public List<StateAndRef<SorterState>> getStates() {
        return rpcOps.vaultQuery(SorterState.class).getStates();
    }

    /**
     * Initiate create flow.
     */
    @PUT
    @Path("sorter-create")
    public Response createSorter(@QueryParam("size") int size, @QueryParam("partyName") String partyName)
            throws InterruptedException, ExecutionException {

        logger.info("API: sorter-create, size: " + size + ", party name: " + partyName);

        if (size <= 0) {
            return Response
                    .status(BAD_REQUEST).entity("Query parameter 'size' must be non-negative.\n").build();
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
                            SorterFlow.Initiator.class, size, SorterContract.Commands.Create.CMD_ID, otherParty)
                    .getReturnValue()
                    .get();

            SorterState sorterState = (SorterState) signedTx.getTx().getOutput(0);
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

    /**
     * Initiate create-and-sort flow.
     */
    @PUT
    @Path("sorter-create-and-sort")
    public Response createAndSortSorter(@QueryParam("size") int size, @QueryParam("partyName") String partyName)
            throws InterruptedException, ExecutionException {

        logger.info("API: sorter-create-and-sort, size: " + size + ", party name: " + partyName);

        if (size <= 0) {
            return Response
                    .status(BAD_REQUEST).entity("Query parameter 'size' must be non-negative.\n").build();
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
                            SorterFlow.Initiator.class, size, SorterContract.Commands.CreateAndSort.CMD_ID, otherParty)
                    .getReturnValue()
                    .get();

            SorterState sorterState = (SorterState) signedTx.getTx().getOutput(0);
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

    /**
     * Initiate sort flow.
     */
    @PUT
    @Path("sorter-sort")
    public Response sortSorter(
            @QueryParam("sorterStateId") String sorterStateId,
            @QueryParam("partyName") String partyName)
            throws InterruptedException, ExecutionException {

        logger.info("API sorter-sort, sorter state: " + sorterStateId + ", party name: " + partyName);

        if (sorterStateId == null || sorterStateId.isEmpty()) {
            return Response
                    .status(BAD_REQUEST)
                    .entity("Query parameter 'sorterStateId' missing or has wrong format.\n")
                    .build();
        }
        if (partyName == null || partyName.isEmpty()) {
            return Response
                    .status(BAD_REQUEST)
                    .entity("Query parameter 'partyName' missing or has wrong format.\n")
                    .build();
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
                            SorterFlow.Initiator.class,
                            sorterStateId,
                            SorterContract.Commands.Sort.CMD_ID,
                            otherParty)
                    .getReturnValue()
                    .get();

            SorterState sorterState = (SorterState) signedTx.getTx().getOutput(0);
            final String msg = String.format(
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
	
	/**
     * Displays all states that are created by Party.
     */
    @GET
    @Path("my-sorters")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMySorters() throws NoSuchFieldException {
        QueryCriteria generalCriteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.ALL);
        Field contractee = SorterSchemaV1.PersistentSorterState.class.getDeclaredField("contractee");
        CriteriaExpression index = Builder.equal(contractee, myLegalName.toString());
        QueryCriteria myCriteria = new QueryCriteria.VaultCustomQueryCriteria(index);
        QueryCriteria criteria = generalCriteria.and(myCriteria);
        List<StateAndRef<SorterState>> results = rpcOps.vaultQueryByCriteria(criteria, SorterState.class).getStates();
        return Response.status(OK).entity(results).build();
    }
}
