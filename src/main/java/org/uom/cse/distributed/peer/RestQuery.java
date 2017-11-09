package org.uom.cse.distributed.peer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uom.cse.distributed.peer.api.EntryTableEntry;
import org.uom.cse.distributed.peer.api.QueryInterface;
import org.uom.cse.distributed.peer.api.RoutingTableEntry;
import org.uom.cse.distributed.peer.utils.HashUtils;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.stream.Stream;

/**
 * Created by Keetmalin on 11/9/2017
 * Project - file-sharer
 */
public class RestQuery implements QueryInterface{

    private static final Logger logger = LoggerFactory.getLogger(Node.class);

    private Node node;
    private Set<InetSocketAddress> inetSocketAddresses;
    private Set<String> queryResultSet;
    private int hopCount;

    @Override
    public Set<InetSocketAddress> searchFullFile(String fileName) {

        inetSocketAddresses.clear();
        hopCount = 0;
        // 1. First look in the same node for the requested file name
        if (searchMyFilesFullName(fileName)) {
            logger.info("file name {} is available in your node itself", fileName);
            inetSocketAddresses.add(new InetSocketAddress(this.node.getIpAddress(), this.node.getPort()));
            return inetSocketAddresses;
        }

        String keywords[] = fileName.split(" ");

        Stream.of(keywords).forEach(keyword -> {
            int nodeId = HashUtils.keywordToNodeId(keyword);

            Optional<RoutingTableEntry> entry = this.node.getRoutingTable().findNodeOrSuccessor(nodeId);
            Optional<RoutingTableEntry> entrySuccessor1 = this.node.getRoutingTable().findSuccessorOf(nodeId);
            Optional<RoutingTableEntry> entrySuccessor2 = this.node.getRoutingTable().findSuccessorOf(entrySuccessor1.get().getNodeId());

            boolean temp = false;
            //if the next node is pointing to the current node
            if (entry.isPresent() && entry.get().getNodeId() == node.getNodeId()) {
                logger.info("searching for the node in Node {}", entry.get().getNodeId());
                inetSocketAddresses = getNodeListSafely(keyword, fileName);
                temp = true;

            } else if (entry.isPresent() && entry.get().getNodeId() != node.getNodeId()) {
                logger.info("searching for the node in Node {}", entry.get().getNodeId());
                inetSocketAddresses = this.node.getCommunicationProvider().searchFullFile(entry.get().getAddress(), fileName, keyword);
                hopCount++;
            } if (inetSocketAddresses.size() == 0 && temp && entrySuccessor1.get().getNodeId() != node.getNodeId()) {
                logger.info("searching for the node in Node {}", entrySuccessor1.get().getNodeId());
                inetSocketAddresses = this.node.getCommunicationProvider().searchFullFile(entrySuccessor1.get().getAddress(), fileName, keyword);
                hopCount++;
            } if (entrySuccessor2.isPresent() && inetSocketAddresses.size() == 0 && entrySuccessor2.get().getNodeId() != node.getNodeId()) {
                logger.info("searching for the node in Node {}", entrySuccessor2.get().getNodeId());
                inetSocketAddresses = this.node.getCommunicationProvider().searchFullFile(entrySuccessor2.get().getAddress(), fileName, keyword);
                hopCount++;
            } else if (inetSocketAddresses.size() != 0){
                logger.info("Entry is not present in the Network");
            }
        });


        logger.info("Search results -> {}", inetSocketAddresses);
        return inetSocketAddresses;
    }

    @Override
    public boolean searchMyFilesFullName(String fileName) {
        for (String s : this.node.getMyFiles()) {
            if (fileName.toLowerCase().equals(s.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int getHopCount() {
        return 0;
    }

    @Override
    public void initialize(Node node) {


        this.node = node;
        this.hopCount = 0;
        this.inetSocketAddresses = new HashSet<>();
        this.queryResultSet = new HashSet<>();

    }

    HashSet<InetSocketAddress> getNodeListSafely(String keyword, String fileName) {
        try {
            return new HashSet<InetSocketAddress>(Arrays.asList(getNodeList(searchEntryTable(keyword, fileName))));
        } catch (Exception e) {
            return new HashSet<InetSocketAddress>();
        }
    }

    private List<String> searchEntryTable(String keyword, String fileName) {
        char c = Character.toUpperCase(keyword.charAt(0));
        List<EntryTableEntry> entryList = this.node.getEntryTable().getEntries().get(c).get(keyword);
        List<String> results = new ArrayList<String>();

        for (EntryTableEntry entry : entryList) {
            if (fileName.toLowerCase().equals(entry.getFileName().toLowerCase())) {
                results.add(entry.getNodeName());
            }
        }
        return results;
    }

    private InetSocketAddress[] getNodeList(List<String> nodeNameList) {
        Set<RoutingTableEntry> entries = this.node.getRoutingTable().getEntries();
        InetSocketAddress[] inetSocketAddresses = new InetSocketAddress[nodeNameList.size()];

        int i = 0;
        for (RoutingTableEntry routingTableEntry : entries) {
            if (nodeNameList.contains(Integer.toString(routingTableEntry.getNodeId()))) {
                inetSocketAddresses[i] = routingTableEntry.getAddress();
                i++;
            }
        }
        return inetSocketAddresses;
    }

}
