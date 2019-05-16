package com.example.analytic.api;

import com.example.analytic.flow.AnalyticsFlow;
import com.example.analytic.contract.AnalyticsContract;
import com.example.analytic.schema.BankAccountSchemaV1;
import com.example.analytic.state.BankAccount;
import com.example.analytic.state.BankTransaction;
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
import java.util.Random;
import java.util.concurrent.ExecutionException;

import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.Response.Status.*;

// This API is accessible from /api/analytic. All paths specified below are relative to it.
@Path("analytic")
public class AnalyticsApi {
    private final CordaRPCOps rpcOps;
    private final CordaX500Name myLegalName;

    private final List<String> serviceNames = ImmutableList.of("Notary");

    static private final Logger logger = LoggerFactory.getLogger(AnalyticsApi.class);

    public AnalyticsApi(CordaRPCOps rpcOps) {
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
     * Displays all BankAccounts states that exist in the node's vault.
     */
    @GET
    @Path("accounts")
    @Produces(MediaType.APPLICATION_JSON)
    public List<StateAndRef<BankAccount>> getAccounts() {
        return rpcOps.vaultQuery(BankAccount.class).getStates();
    }

    /**
     * Displays all BankTransactions states that exist in the node's vault.
     */
    @GET
    @Path("transactions")
    @Produces(MediaType.APPLICATION_JSON)
    public List<StateAndRef<BankTransaction>> getTransactions() {
        return rpcOps.vaultQuery(BankTransaction.class).getStates();
    }


    /**
     * Initiate create flow.
     */
    @PUT
    @Path("deploy")
    public Response deploy(
            @QueryParam("accounts") int accounts,
            @QueryParam("transactions") int transactions,
            @QueryParam("partyName") String partyName)
            throws InterruptedException, ExecutionException {

        logger.info("API: analytics deploy, party name: " + partyName);

        if (accounts < 1 || transactions < 1) {
            return Response
                    .status(BAD_REQUEST).entity("Query parameter 'accounts' or 'transactions' are invalid.\n").build();
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

        /*
        try {
            final SignedTransaction signedTx = rpcOps
                    .startTrackedFlowDynamic(
                            AnalyticsFlow.Initiator.class, accounts, AnalyticsContract.Commands.Init.CMD_ID, otherParty)
                    .getReturnValue()
                    .get();
            logger.info("New batch of BankAccounts with Corda Tx " + signedTx.getId().toString());
        } catch (Throwable ex) {
            logger.error(ex.getMessage(), ex);
        }
        */

        for (int i = 0; i < accounts; i++) {
            try {
                final SignedTransaction signedTx = rpcOps
                        .startTrackedFlowDynamic(
                                AnalyticsFlow.Initiator.class, i, AnalyticsContract.Commands.Create.CMD_ID, otherParty)
                        .getReturnValue()
                        .get();
                logger.info("New BankAccount index " + i + " Corda Tx " + signedTx.getId().toString());
            } catch (Throwable ex) {
                logger.error(ex.getMessage(), ex);
            }
        }

        Random rand = new Random(System.currentTimeMillis());

        for (int i = 0; i < transactions; i++) {
            int fromAcc = rand.nextInt(accounts);
            int toAcc = rand.nextInt(accounts);
            if (fromAcc == toAcc)
                toAcc = (toAcc + 1) % accounts;
            int val = 100;

            try {
                final SignedTransaction signedTx = rpcOps
                        .startTrackedFlowDynamic(
                                AnalyticsFlow.Initiator.class, fromAcc, toAcc, val, AnalyticsContract.Commands.Send.CMD_ID, otherParty)
                        .getReturnValue()
                        .get();
                logger.info("New BankTransaction index " + i + " Corda Tx " + signedTx.getId().toString());
            } catch (Throwable ex) {
                logger.error(ex.getMessage(), ex);
            }
        }

        List<StateAndRef<BankTransaction>> txList = getTransactions();
        for (StateAndRef<BankTransaction> tx : txList) {
            try {
                final SignedTransaction signedTx = rpcOps
                        .startTrackedFlowDynamic(
                                AnalyticsFlow.Initiator.class, tx.getState().getData().getLinearId().getId().toString(), AnalyticsContract.Commands.Commit.CMD_ID, otherParty)
                        .getReturnValue()
                        .get();
                logger.info("New Commit with Corda Tx " + signedTx.getId().toString());
            } catch (Throwable ex) {
                logger.error(ex.getMessage(), ex);
            }
        }

        return Response
                .status(CREATED).entity("Deploy done.\n").build();
    }

    /**
     * Initiate create flow.
     */
    @GET
    @Path("query2")
    @Produces(MediaType.APPLICATION_JSON)
    public List<StateAndRef<BankAccount>> query2(
            @QueryParam("account") int account,
            @QueryParam("version") int version) {

        logger.info("API: analytics query2, account id: " + account + ", version: " + version);

        try {
            Field accId = BankAccountSchemaV1.PersistentBankAccount.class.getDeclaredField("accountId");
            CriteriaExpression index1 = Builder.equal(accId, account);
            Field ver = BankAccountSchemaV1.PersistentBankAccount.class.getDeclaredField("version");
            CriteriaExpression index2 = Builder.equal(ver, version);
            QueryCriteria myCriteria = new QueryCriteria.VaultCustomQueryCriteria(index1);
            myCriteria = myCriteria.and(new QueryCriteria.VaultCustomQueryCriteria(index2));
            return rpcOps.vaultQueryByCriteria(myCriteria, BankAccount.class).getStates();
        }
        catch (Exception e) {
            return null;
        }
    }
}
