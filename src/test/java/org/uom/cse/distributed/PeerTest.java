/*
 * Copyright to Eduze@UoM 2017
 */

package org.uom.cse.distributed;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.uom.cse.distributed.peer.DistributedNode;
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
            port++;
        }
    }

    @AfterClass
    public static void tearDown() {
        bootstrapServer.stop();
    }
}
