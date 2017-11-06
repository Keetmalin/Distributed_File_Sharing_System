/* 
 * <Paste your header here>
 */
package org.uom.cse.distributed.peer.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.ArrayList;
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

    private final Set<RoutingTableEntry> entries = new HashSet<>();
    private final List<RoutingTableListener> listeners = new ArrayList<>();

    public Set<RoutingTableEntry> getEntries() {
        return new HashSet<>(entries);
    }

    public synchronized void addEntry(RoutingTableEntry entry) {
        List<RoutingTableEntry> duplicates = this.entries.stream()
                .filter(e -> e.getAddress().equals(entry.getAddress()))
                .collect(Collectors.toList());

        if (duplicates.size() == 0) {
            logger.debug("Adding entry: {} to the routing table", entry);
            this.entries.add(entry);
            notifyListeners(entry, true);
        } else if (duplicates.stream().filter(e -> e.getNodeId() == entry.getNodeId()).count() == 1) {
            logger.warn("Entry : {} already exists", entry);
        } else {
            // We have an erroneous entry. Correct it.
            RoutingTableEntry e = duplicates.get(0);
            logger.warn("Correcting entry {} to {}", e, entry);
            e.setNodeId(entry.getNodeId());
            notifyListeners(entry, true);
        }
    }

    public synchronized boolean removeEntry(RoutingTableEntry e) {
        if (this.entries.remove(e)) {
            logger.info("Removed entry -> {}", e);
            notifyListeners(e, false);
            return true;
        }

        return false;
    }

    public synchronized boolean removeEntry(InetSocketAddress node) {
        Optional<RoutingTableEntry> entry = this.entries.stream()
                .filter(e -> e.getAddress().getAddress().equals(node.getAddress()) &&
                        e.getAddress().getPort() == node.getPort())
                .findFirst();

        if (entry.isPresent()) {
            this.entries.remove(entry.get());
            logger.info("Removed entry -> {}", entry);
            notifyListeners(entry.get(), false);
            return true;
        }

        return false;
    }

    /**
     * Removes all the entries in the routing table and clears it.
     */
    public synchronized void clear() {
        this.entries.clear();
    }

    /**
     * Finds the {@link InetSocketAddress} of a given node. Searched by the {@link RoutingTableEntry#nodeId}
     *
     * @param nodeId ID of the node of which IP-port info is required to be found
     * @return Optional of {@link InetSocketAddress}
     */
    public Optional<RoutingTableEntry> findByNodeId(int nodeId) {
        return this.entries.stream()
                .filter(e -> e.getNodeId() == nodeId)
                .findFirst();
    }

    /**
     * Finds the routing table entry corresponding to the nodeId. The entry can be the exact node or the successor of
     * that node.
     *
     * @param nodeId Node name to be found in the routing table
     * @return optional of entry
     */
    public Optional<RoutingTableEntry> findNodeOrSuccessor(int nodeId) {
        List<RoutingTableEntry> sortedEntries = this.entries.stream()
                .sorted(Comparator.comparingInt(RoutingTableEntry::getNodeId))
                .collect(Collectors.toList());

        Optional<RoutingTableEntry> successor = sortedEntries.stream()
                .filter(e -> e.getNodeId() >= nodeId)
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
                .sorted(Comparator.comparingInt(RoutingTableEntry::getNodeId))
                .collect(Collectors.toList());

        Optional<RoutingTableEntry> successor = sortedEntries.stream()
                .filter(e -> e.getNodeId() > nodeId)
                .findFirst();

        if (successor.isPresent() && successor.get().getNodeId() != nodeId) {
            return successor;
        } else if (sortedEntries.size() > 0 && sortedEntries.get(0).getNodeId() != nodeId) {
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
                .sorted(Comparator.comparingInt(e -> ((RoutingTableEntry) e).getNodeId()).reversed())
                .collect(Collectors.toList());

        Optional<RoutingTableEntry> predecessor = sortedEntries.stream()
                .filter(e -> e.getNodeId() < nodeId)
                .findFirst();

        if (predecessor.isPresent() && predecessor.get().getNodeId() != nodeId) {
            return predecessor;
        } else if (sortedEntries.size() > 0 && sortedEntries.get(0).getNodeId() != nodeId) {
            return Optional.of(sortedEntries.get(0));
        }

        return Optional.empty();
    }

    private void notifyListeners(RoutingTableEntry entry, boolean added) {
        this.listeners.forEach(listener -> {
            if (added) {
                listener.entryAdded(entry);
            } else {
                listener.entryRemoved(entry);
            }
        });
    }

    public void addListener(RoutingTableListener listener) {
        if (!this.listeners.contains(listener)) {
            this.listeners.add(listener);
        }
    }

    public void removeListener(RoutingTableListener listener) {
        this.listeners.remove(listener);
    }
}
