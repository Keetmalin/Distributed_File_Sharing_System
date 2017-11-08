package org.uom.cse.distributed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.uom.cse.distributed.peer.Node;
import org.uom.cse.distributed.performance.PerformanceMeasure;

import java.util.ArrayList;
import java.util.List;

import static org.uom.cse.distributed.peer.api.State.CONFIGURED;
import static org.uom.cse.distributed.peer.api.State.IDLE;

/**
 * @author  Keet Sugathadasa
 */
public class PerformanceTest {

    private static final Logger logger = LoggerFactory.getLogger(Node.class);
    private static List<Node> nodes = new ArrayList<>();

    private static final int testCount = 20;

    public static void main(String[] args) {
        SocketBasedPeerTest socketBasedPeerTest = new SocketBasedPeerTest();
        socketBasedPeerTest.socketBasedSetUp();

        nodes = socketBasedPeerTest.getNodes();

        int i = 0;
        for (Node node : nodes) {
            logger.info("\n------------------- Starting Node at port {} --------------------", node.getPort());
            node.start();

            i++;
        }

        Node mainNode = nodes.get(0);
        PerformanceMeasure performanceMeasure = new PerformanceMeasure(mainNode);
        float result = performanceMeasure.calculateQueryLatency(testCount);

        logger.info("Latency in average = {} milliseconds" , result);

        logger.info("--------------------------------------------------------------------------\n\n");

        float result2 = performanceMeasure.calculateQueryHopCount(testCount);

        logger.info("Query Hop Count in average = {} hops" , result2);

        nodes.forEach(Node::stop);

        socketBasedPeerTest.tearDown();

    }
}
