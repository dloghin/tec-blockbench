package com.example.smallbank.api;

import com.example.smallbank.contract.SmallBankContract;
import com.example.smallbank.state.SmallBankState;
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
import com.example.smallbank.flow.SmallBankFlow;
import com.example.smallbank.schema.SmallBankSchemaV1;
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

// This API is accessible from /api/smallbank. All paths specified below are relative to it.
@Path("smallbank")
public class SmallBankApi {
    private final CordaRPCOps rpcOps;
    private final CordaX500Name myLegalName;

    private final List<String> serviceNames = ImmutableList.of("Notary");

    static private final Logger logger = LoggerFactory.getLogger(SmallBankApi.class);

    public SmallBankApi(CordaRPCOps rpcOps) {
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
     * Displays all states that exist in the node's vault.
     */
    @GET
    @Path("accounts")
    @Produces(MediaType.APPLICATION_JSON)
    public List<StateAndRef<SmallBankState>> getStates() {
        return rpcOps.vaultQuery(SmallBankState.class).getStates();
    }

    /**
     * Initiate flow.
     */
    @PUT
    @Path("smallbank")
    public Response smallbank(
            @QueryParam("partyName") String partyName,
            @QueryParam("command") int command,
            @QueryParam("account1") int account1,
            @QueryParam("account2") int account2,
            @QueryParam("amount") int amount)
            throws InterruptedException, ExecutionException {

        logger.info("API: smallbank, command: " + command + ", party name: " + partyName);

        if (command < SmallBankContract.Commands.Amalgate.CMD_ID || command > SmallBankContract.Commands.WriteCheck.CMD_ID) {
            return Response
                    .status(BAD_REQUEST).entity("Query parameter 'command' is not valid.\n").build();
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

        if (command == SmallBankContract.Commands.GetBalance.CMD_ID) {
            if (account1 == 0)
                return Response
                        .status(BAD_REQUEST).entity("Invalid account.\n").build();
            StateAndRef<SmallBankState> stateAndRef = SmallBankFlow.Initiator.queryState(null, rpcOps, SmallBankState.CheckingsTab, account1);
            if (stateAndRef != null && stateAndRef.getState().getData() != null) {
                return Response.status(CREATED).entity("Account balance: " + stateAndRef.getState().getData().getBalance()).build();
            }
            return Response
                    .status(BAD_REQUEST).entity("Account not found.\n").build();
        }

        try {
            final SignedTransaction signedTx = rpcOps
                    .startTrackedFlowDynamic(
                            SmallBankFlow.Initiator.class, account1, account2, amount, command, otherParty)
                    .getReturnValue()
                    .get();

            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < signedTx.getTx().getOutputs().size(); i++) {
                SmallBankState smallBankState = (SmallBankState) signedTx.getTx().getOutput(0);
                sb.append("Output state id ").append(smallBankState.getLinearId().toString()).append(". ");
            }
            final String msg =
                    String.format(
                            "Transaction id %s committed to ledger. %s\n",
                            signedTx.getId(),
                            sb.toString());
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
    @Path("my-states")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMySorters() throws NoSuchFieldException {
        QueryCriteria generalCriteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.ALL);
        Field owner = SmallBankSchemaV1.PersistentSmallBankState.class.getDeclaredField("owner");
        CriteriaExpression index = Builder.equal(owner, myLegalName.toString());
        QueryCriteria myCriteria = new QueryCriteria.VaultCustomQueryCriteria(index);
        QueryCriteria criteria = generalCriteria.and(myCriteria);
        List<StateAndRef<SmallBankState>> results = rpcOps.vaultQueryByCriteria(criteria, SmallBankState.class).getStates();
        return Response.status(OK).entity(results).build();
    }
}
