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
import org.uom.cse.distributed.peer.RoutingTableEntry;
import org.uom.cse.distributed.peer.api.State;
import org.uom.cse.distributed.peer.utils.HashUtils;
import org.uom.cse.distributed.server.BootstrapServer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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

            checkNodes(nodes);
        }

        checkNodes(nodes);

        nodes.forEach(node -> {
            node.stop();
            Assert.assertEquals(node.getState(), State.IDLE);
            Assert.assertEquals(node.getRoutingTable().getEntries().size(), 0);
        });
    }

    private void checkNodes(List<Node> nodes) {
        nodes.stream()
                .sorted(Comparator.comparingInt(Node::getNodeId))
                .forEach(node -> {
                    logger.debug("{} -> {}\t: Characters -> {}", node.getNodeId(), node.getMyChar(),
                            node.getEntryTable().getEntries().keySet());
                });

        for (Node node : nodes) {
            Optional<RoutingTableEntry> myPredecessor = node.getRoutingTable().findPredecessorOf(node.getNodeId());
            if (node.getRoutingTable().getEntries().size() > 1) {
                Assert.assertTrue(myPredecessor.isPresent());
            } else {
                continue;
            }

            Set<Character> characters = HashUtils.findCharactersOf(node.getNodeId(),
                    Integer.parseInt(myPredecessor.get().getNodeName()));
            for (Character character : characters) {
                Assert.assertNotNull(node.getEntryTable().getKeywordsFor(character));
            }

            Assert.assertEquals(characters.size() + 1, node.getEntryTable().getEntries().size());
        }
    }

    @AfterClass
    public static void tearDown() {
        bootstrapServer.stop();
    }
}
