package org.uom.cse.distributed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeMethod;
import org.uom.cse.distributed.peer.Node;
import org.uom.cse.distributed.server.BootstrapServer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

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
            nodes.add(new Node(port + i));
        }
    }
}
