package com.example.sorter;

import com.google.common.collect.ImmutableList;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.utilities.NetworkHostAndPort;
import net.corda.testing.core.TestIdentity;
import net.corda.testing.driver.DriverParameters;
import net.corda.testing.driver.NodeHandle;
import net.corda.testing.driver.NodeParameters;
import net.corda.testing.driver.WebserverHandle;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.Test;

import java.util.List;

import static net.corda.testing.driver.Driver.driver;
import static org.junit.Assert.assertEquals;

public class DriverBasedTests {
    private final TestIdentity partyA = new TestIdentity(new CordaX500Name("PartyA", "", "GB"));
    private final TestIdentity partyB = new TestIdentity(new CordaX500Name("PartyB", "", "US"));

    @Test
    public void nodeTest() {
        driver(new DriverParameters().withIsDebug(true).withStartNodesInProcess(true), dsl -> {

            // This starts three nodes simultaneously with startNode, which returns a future that completes when the node
            // has completed startup. Then these are all resolved with getOrThrow which returns the NodeHandle list.
            List<CordaFuture<NodeHandle>> handleFutures = ImmutableList.of(
                    dsl.startNode(new NodeParameters().withProvidedName(partyA.getName())),
                    dsl.startNode(new NodeParameters().withProvidedName(partyB.getName()))
            );

            try {
                NodeHandle partyAHandle = handleFutures.get(0).get();
                NodeHandle partyBHandle = handleFutures.get(1).get();

                // This test will call via the RPC proxy to find a party of another node to verify that the nodes have
                // started and can communicate. This is a very basic test, in practice tests would be starting flows,
                // and verifying the states in the vault and other important metrics to ensure that your CorDapp is working
                // as intended.
                assertEquals(partyAHandle.getRpc().wellKnownPartyFromX500Name(partyB.getName()).getName(), partyB.getName());
                assertEquals(partyBHandle.getRpc().wellKnownPartyFromX500Name(partyA.getName()).getName(), partyA.getName());
            } catch (Exception e) {
                throw new RuntimeException("Caught exception during test", e);
            }

            return null;
        });
    }

    @Test
    public void nodeWebserverTest() {
        driver(new DriverParameters().withIsDebug(true).withStartNodesInProcess(true), dsl -> {

            List<CordaFuture<NodeHandle>> handleFutures = ImmutableList.of(
                    dsl.startNode(new NodeParameters().withProvidedName(partyA.getName())),
                    dsl.startNode(new NodeParameters().withProvidedName(partyB.getName()))
            );

            try {
                // This test starts each node's webserver and makes an HTTP call to retrieve the body of a GET endpoint on
                // the node's webserver, to verify that the nodes' webservers have started and have loaded the API.
                for (CordaFuture<NodeHandle> handleFuture : handleFutures) {
                    NodeHandle nodeHandle = handleFuture.get();

                    WebserverHandle webserverHandle = dsl.startWebserver(nodeHandle).get();

                    NetworkHostAndPort nodeAddress = webserverHandle.getListenAddress();
                    String url = String.format("http://%s/api/sorter/sorters", nodeAddress);

                    Request request = new Request.Builder().url(url).build();
                    OkHttpClient client = new OkHttpClient();
                    Response response = client.newCall(request).execute();

                    assertEquals("[ ]", response.body().string());
                }
            } catch (Exception e) {
                throw new RuntimeException("Caught exception during test", e);
            }

            return null;
        });
    }
}