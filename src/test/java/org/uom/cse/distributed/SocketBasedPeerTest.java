/*
 * Copyright to Eduze@UoM 2017
 */

package org.uom.cse.distributed;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uom.cse.distributed.peer.Node;
import org.uom.cse.distributed.peer.api.State;
import org.uom.cse.distributed.server.BootstrapServer;

import java.util.ArrayList;
import java.util.List;

public class SocketBasedPeerTest {

    private static final Logger logger = LoggerFactory.getLogger(SocketBasedPeerTest.class);

    private static BootstrapServer bootstrapServer;

    @BeforeClass
    public static void setUp() {
        bootstrapServer = new BootstrapServer(Constants.BOOTSTRAP_PORT);
        bootstrapServer.start();
    }

    @Test
    public void testRegisterWithBootstrapServer() {
        List<Node> nodes = new ArrayList<>();
        int port = 35002;
        for (int i = 0; i < 10; i++) {
            logger.info("\n------------------- Creating Node at port {} --------------------", port);
            Node node = new Node(port);
            nodes.add(node);
            node.start();

            Assert.assertEquals(node.getState(), State.CONFIGURED);
            Assert.assertEquals(node.getRoutingTable().getEntries().size(), i + 1);

            port++;
        }

        nodes.forEach(node -> {
            node.stop();
            Assert.assertEquals(node.getState(), State.IDLE);
            Assert.assertEquals(node.getRoutingTable().getEntries().size(), 0);
        });
    }

    @AfterClass
    public static void tearDown() {
        bootstrapServer.stop();
    }
}
