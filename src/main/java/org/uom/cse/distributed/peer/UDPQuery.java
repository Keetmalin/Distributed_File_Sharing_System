package org.uom.cse.distributed.peer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uom.cse.distributed.peer.api.QueryInterface;
import org.uom.cse.distributed.peer.utils.HashUtils;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * This class implements the query part of the system where nodes can query
 * for the files in need and do a look up
 *
 * @author Keet Sugathadasa
 */
public class UDPQuery implements QueryInterface {

    private static final Logger logger = LoggerFactory.getLogger(Node.class);

    private Node node;

    public void initialize(Node node){
        this.node = node;
    }

    @Override
    public InetSocketAddress[] searchFullFile(String fileName) {

        // 1) First look in the same node for the requested file name
        if (searchMyFilesFullName(fileName)){
            logger.info("file name {} is available in your node itself");
            //TODO check what to return if the file is self contained
            InetSocketAddress[] inetSocketAddresses = new InetSocketAddress[1];
            inetSocketAddresses[0] = new InetSocketAddress(this.node.getIpAddress(), this.node.getPort());
            return inetSocketAddresses;
        }
        String keywords[] = fileName.split(" ");
        Stream.of(keywords).forEach(keyword -> {

            int nodeId = HashUtils.keywordToNodeId(keyword);
            Optional<RoutingTableEntry> entry = this.node.getRoutingTable().findNodeOrSuccessor(String.valueOf(nodeId));

            // the entry should be a different node (not itself)
            if (entry.isPresent()) {
                logger.debug("searching for the node in Node {}" , entry.get().getNodeName());
//                this.node.getCommunicationProvider()


            } else {
                logger.debug("Entry is not present");
            }
        });

        return new InetSocketAddress[0];
    }

    @Override
    public boolean searchMyFilesFullName(String fileName) {
        return this.node.getMyFiles().contains(fileName);
    }


}
