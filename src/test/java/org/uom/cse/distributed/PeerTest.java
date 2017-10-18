/*
 * Copyright to Eduze@UoM 2017
 */

package org.uom.cse.distributed;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.uom.cse.distributed.peer.DistributedNode;
import org.uom.cse.distributed.peer.api.State;
import org.uom.cse.distributed.server.BootstrapServer;

import java.util.ArrayList;
import java.util.List;

public class PeerTest {

    private static BootstrapServer bootstrapServer;

    @BeforeClass
    public static void setUp() {
        bootstrapServer = new BootstrapServer(Constants.BOOTSTRAP_PORT);
        bootstrapServer.start();
    }

    @Test
    public void testRegisterWithBootstrapServer() {
        List<DistributedNode> nodes = new ArrayList<>();
        int port = 35002;
        for (int i = 0; i < 10; i++) {
            DistributedNode node = new DistributedNode(port);
            nodes.add(node);
            node.start();

            Assert.assertEquals(node.getState(), State.REGISTERED);
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
            Assert.assertEquals(node.getState(), State.STOPPED);
            Assert.assertEquals(node.getPeers().size(), 0);
        });
    }

    @AfterClass
    public static void tearDown() {
        bootstrapServer.stop();
    }
}
