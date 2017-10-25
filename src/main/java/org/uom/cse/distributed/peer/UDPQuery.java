package org.uom.cse.distributed.peer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uom.cse.distributed.peer.api.QueryInterface;
import org.uom.cse.distributed.peer.api.RoutingTableEntry;
import org.uom.cse.distributed.peer.utils.HashUtils;

import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * This class implements the query part of the system where nodes can query for the files in need and do a look up
 *
 * @author Keet Sugathadasa
 */
public class UDPQuery implements QueryInterface {

    private static final Logger logger = LoggerFactory.getLogger(Node.class);

    private Node node;
    private Set<InetSocketAddress> inetSocketAddresses;

    public void initialize(Node node) {
        this.node = node;
    }

    @Override
    public void searchFullFile(String fileName) {
        // 1. First look in the same node for the requested file name
        if (searchMyFilesFullName(fileName)) {
            logger.info("file name {} is available in your node itself", fileName);
            inetSocketAddresses.add(new InetSocketAddress(this.node.getIpAddress(), this.node.getPort()));
            return;
        }

        String keywords[] = fileName.split(" ");

        Stream.of(keywords).forEach(keyword -> {
            int nodeId = HashUtils.keywordToNodeId(keyword);
            Optional<RoutingTableEntry> entry = this.node.getRoutingTable().findNodeOrSuccessor(nodeId);

            // the entry should be a different node (not itself)
            if (entry.isPresent() && Integer.parseInt(entry.get().getNodeName()) != node.getNodeId()) {
                logger.debug("searching for the node in Node {}", entry.get().getNodeName());
                inetSocketAddresses = this.node.getCommunicationProvider().searchFullFile(entry.get().getAddress(), fileName, keyword);
            } else {
                logger.debug("Entry is not present");
            }
        });

        logger.info("Search results -> {}", inetSocketAddresses);
    }

    @Override
    public boolean searchMyFilesFullName(String fileName) {
        return this.node.getMyFiles().contains(fileName);
    }
}
