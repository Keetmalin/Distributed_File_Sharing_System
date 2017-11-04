package org.uom.cse.distributed;

import org.glassfish.jersey.client.JerseyClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.uom.cse.distributed.peer.Node;
import org.uom.cse.distributed.peer.RestServices.NodeController;
import org.uom.cse.distributed.peer.RestServices.RestCommunicationProvider;
import org.uom.cse.distributed.peer.RestServices.RestNodeServer;
import org.uom.cse.distributed.peer.api.RoutingTable;
import org.uom.cse.distributed.peer.api.RoutingTableEntry;
import org.uom.cse.distributed.peer.utils.HashUtils;
import org.uom.cse.distributed.server.BootstrapServer;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.uom.cse.distributed.peer.api.State.CONFIGURED;
import static org.uom.cse.distributed.peer.api.State.IDLE;

public class RESTServerTest {
    private static final Logger logger = LoggerFactory.getLogger(RESTServerTest.class);

    private static final int NODE_COUNT = 6;
    private static BootstrapServer bootstrapServer;

    private AtomicInteger atomicInteger = new AtomicInteger(0);
    private final List<Node> nodes = new ArrayList<>();

    @BeforeMethod
    public void setUp() {
        bootstrapServer = new BootstrapServer(Constants.BOOTSTRAP_PORT);
        bootstrapServer.start();

        int port = 35002;
        for (int i = 0; i < NODE_COUNT; i++) {
            RestNodeServer restNodeServer= new RestNodeServer();
            RestCommunicationProvider communicationProvider= new RestCommunicationProvider();
            nodes.add(new Node(port + i,communicationProvider,restNodeServer));
        }
    }

    @Test
    public void getCameraIdTest() {
        Client client = JerseyClientBuilder.createClient();

        UriBuilder builder = UriBuilder.fromPath("api")
                .scheme("http")
                .host("localhost")
                .port(35002)
                .path("v1")
                .path("nodecontroller")
                .path("RouteTable");

        RoutingTable table = client.target(builder)
                .request(MediaType.APPLICATION_JSON)
                .get(RoutingTable.class);

    }

    @AfterMethod
    public void tearDown() {
        nodes.forEach(node -> {
            if (!node.getState().equals(IDLE)) {
                node.stop();
            }
        });

        nodes.clear();
        bootstrapServer.stop();
    }
}
