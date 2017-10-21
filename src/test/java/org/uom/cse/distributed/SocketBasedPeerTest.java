/*
 * Copyright to Eduze@UoM 2017
 */

package org.uom.cse.distributed;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.uom.cse.distributed.peer.Node;
import org.uom.cse.distributed.peer.SocketBasedCommunicationProvider;
import org.uom.cse.distributed.peer.api.State;
import org.uom.cse.distributed.server.BootstrapServer;

import java.util.ArrayList;
import java.util.List;

public class SocketBasedPeerTest {

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
            Node node = new Node(port, new SocketBasedCommunicationProvider());
            nodes.add(node);
            node.start();

            Assert.assertEquals(node.getState(), State.CONNECTED);
            if (i == 1) {
                Assert.assertEquals(node.getPeers().size(), 1);
                Assert.assertEquals(node.getPeers().get(0).getPort(), port - 1);
            } else if (i > 1) {
                Assert.assertEquals(node.getPeers().size(), 2);
            }
            port++;
        }

        nodes.forEach(node -> {
            node.stop();
            Assert.assertEquals(node.getState(), State.IDLE);
            Assert.assertEquals(node.getPeers().size(), 0);
        });
    }

    @AfterClass
    public static void tearDown() {
        bootstrapServer.stop();
    }
}
