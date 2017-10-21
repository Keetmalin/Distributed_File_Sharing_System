/* 
 * <Paste your header here>
 */
package org.uom.cse.distributed.peer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Collections;
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
 */
public class RoutingTable {

    private static final Logger logger = LoggerFactory.getLogger(RoutingTable.class);

    private final Set<RoutingTableEntry> entries = Collections.synchronizedSet(new HashSet<>());

    public Set<RoutingTableEntry> getEntries() {
        return entries;
    }

    public void addEntry(RoutingTableEntry routingTableEntry) {
        List<RoutingTableEntry> duplicates = this.entries.stream()
                .filter(e -> e.getAddress().equals(routingTableEntry.getAddress()))
                .collect(Collectors.toList());

        if (duplicates.size() == 0) {
            this.entries.add(routingTableEntry);
        } else if (duplicates.stream().filter(e -> e.getNodeName().equals(routingTableEntry.getNodeName())).count() == 1) {
            logger.warn("RoutingTableEntry : {} already exists", routingTableEntry);
        } else {
            // We have an erroneous routingTableEntry. Correct it.
            RoutingTableEntry e = duplicates.get(0);
            logger.warn("Correcting routingTableEntry {} to {}", e, routingTableEntry);
            e.setNodeName(routingTableEntry.getNodeName());
        }
    }

    public boolean removeEntry(RoutingTableEntry e) {
        return this.entries.remove(e);
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


}
