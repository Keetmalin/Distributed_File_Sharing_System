/* 
 * <Paste your header here>
 */
package org.uom.cse.distributed.peer.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Represents the routing table of a given node. Each entry wil have 3 items;
 * <pre>
 *     <ul>
 *         <li>IP Address</li>
 *         <li>Port</li>
 *         <li>Node Name - This is a number which indicates the location in the DHT</li>
 *     </ul>
 * </pre>
 *
 * @author Imesha Sudasingha
 * @author Keet Sugathadasa
 */
public class RoutingTable {

    private static final Logger logger = LoggerFactory.getLogger(RoutingTable.class);

    private final Set<RoutingTableEntry> entries = Collections.synchronizedSet(new HashSet<>());

    public Set<RoutingTableEntry> getEntries() {
        return new HashSet<>(entries);
    }

    public void addEntry(RoutingTableEntry entry) {
        List<RoutingTableEntry> duplicates = this.entries.stream()
                .filter(e -> e.getAddress().equals(entry.getAddress()))
                .collect(Collectors.toList());

        if (duplicates.size() == 0) {
            logger.debug("Adding entry: {} to the routing table", entry);
            this.entries.add(entry);
        } else if (duplicates.stream().filter(e -> e.getNodeName().equals(entry.getNodeName())).count() == 1) {
            logger.warn("Entry : {} already exists", entry);
        } else {
            // We have an erroneous entry. Correct it.
            RoutingTableEntry e = duplicates.get(0);
            logger.warn("Correcting entry {} to {}", e, entry);
            e.setNodeName(entry.getNodeName());
        }
    }

    public boolean removeEntry(RoutingTableEntry e) {
        return this.entries.remove(e);
    }

    /**
     * Removes all the entries in the routing table and clears it.
     */
    public void clear() {
        this.entries.clear();
    }

    /**
     * Finds the {@link InetSocketAddress} of a given node. Searched by the {@link RoutingTableEntry#nodeName}
     *
     * @param nodeName Name of the node of which IP-port info is required to be found
     * @return Optional of {@link InetSocketAddress}
     */
    public Optional<RoutingTableEntry> findByNodeName(String nodeName) {
        return this.entries.stream()
                .filter(e -> e.getNodeName().equals(nodeName))
                .findFirst();
    }

    /**
     * Finds the routing table entry corresponding to the nodeId. The entry can be the exact node of the successor of
     * that node.
     *
     * @param nodeId Node name to be found in the routing table
     * @return optional of entry
     */
    public Optional<RoutingTableEntry> findNodeOrSuccessor(int nodeId) {
        List<RoutingTableEntry> sortedEntries = this.entries.stream()
                .sorted(Comparator.comparingInt(e -> Integer.parseInt(e.getNodeName())))
                .collect(Collectors.toList());

        Optional<RoutingTableEntry> successor = sortedEntries.stream()
                .filter(e -> Integer.parseInt(e.getNodeName()) >= nodeId)
                .findFirst();

        if (successor.isPresent()) {
            return successor;
        } else if (sortedEntries.size() > 0) {
            return Optional.of(sortedEntries.get(0));
        } else {
            return Optional.empty();
        }
    }

    /**
     * Finds the successor of a given node
     *
     * @param nodeId ID of the node of which we want to find the successor
     * @return Optional
     */
    public Optional<RoutingTableEntry> findSuccessorOf(int nodeId) {
        List<RoutingTableEntry> sortedEntries = this.entries.stream()
                .sorted(Comparator.comparingInt(e -> Integer.parseInt(e.getNodeName())))
                .collect(Collectors.toList());

        Optional<RoutingTableEntry> successor = sortedEntries.stream()
                .filter(e -> Integer.parseInt(e.getNodeName()) > nodeId)
                .findFirst();

        if (successor.isPresent() && Integer.parseInt(successor.get().getNodeName()) != nodeId) {
            return successor;
        } else if (sortedEntries.size() > 0 && Integer.parseInt(sortedEntries.get(0).getNodeName()) != nodeId) {
            return Optional.of(sortedEntries.get(0));
        }

        return Optional.empty();
    }

    /**
     * Finds the predecessor of a given node
     *
     * @param nodeId ID of the node of which we want to find the successor
     * @return Optional
     */
    public Optional<RoutingTableEntry> findPredecessorOf(int nodeId) {
        List<RoutingTableEntry> sortedEntries = this.entries.stream()
                .sorted(Comparator.comparingInt(e -> Integer.parseInt(((RoutingTableEntry) e).getNodeName())).reversed())
                .collect(Collectors.toList());

        Optional<RoutingTableEntry> predecessor = sortedEntries.stream()
                .filter(e -> Integer.parseInt(e.getNodeName()) < nodeId)
                .findFirst();

        if (predecessor.isPresent() && Integer.parseInt(predecessor.get().getNodeName()) != nodeId) {
            return predecessor;
        } else if (sortedEntries.size() > 0 && Integer.parseInt(sortedEntries.get(0).getNodeName()) != nodeId) {
            return Optional.of(sortedEntries.get(0));
        }

        return Optional.empty();
    }
}
