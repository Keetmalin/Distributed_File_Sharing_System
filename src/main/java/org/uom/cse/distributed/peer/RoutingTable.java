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

    private final Set<Entry> entries = Collections.synchronizedSet(new HashSet<>());

    public Set<Entry> getEntries() {
        return entries;
    }

    public void addEntry(Entry entry) {
        List<Entry> duplicates = this.entries.stream()
                .filter(e -> e.getAddress().equals(entry.getAddress()))
                .collect(Collectors.toList());

        if (duplicates.size() == 0) {
            this.entries.add(entry);
        } else if (duplicates.stream().filter(e -> e.getNodeName().equals(entry.getNodeName())).count() == 1) {
            logger.warn("Entry : {} already exists", entry);
        } else {
            // We have an erroneous entry. Correct it.
            Entry e = duplicates.get(0);
            logger.warn("Correcting entry {} to {}", e, entry);
            e.setNodeName(entry.getNodeName());
        }
    }

    public boolean removeEntry(Entry e) {
        return this.entries.remove(e);
    }

    /**
     * Finds the {@link InetSocketAddress} of a given node. Searched by the {@link Entry#nodeName}
     *
     * @param nodeName Name of the node of which IP-port info is required to be found
     * @return Optional of {@link InetSocketAddress}
     */
    public Optional<Entry> findByNodeName(String nodeName) {
        return this.entries.stream()
                .filter(e -> e.getNodeName().equals(nodeName))
                .findFirst();
    }

    /**
     * Represents an entry in the routing table. Consists of IP, port and Node name.
     */
    public class Entry {
        private InetSocketAddress address;
        private String nodeName;

        public Entry(InetSocketAddress address, String nodeName) {
            if (address == null || nodeName == null) {
                throw new IllegalArgumentException("Address and Node name should not be null");
            }

            this.address = address;
            this.nodeName = nodeName;
        }

        public InetSocketAddress getAddress() {
            return address;
        }

        public String getNodeName() {
            return nodeName;
        }

        public void setNodeName(String nodeName) {
            this.nodeName = nodeName;
        }

        @Override
        public boolean equals(Object o) {
            return o != null &&
                    o instanceof Entry &&
                    this.getNodeName().equals(((Entry) o).getNodeName()) &&
                    this.getAddress().equals(((Entry) o).getAddress());
        }

        @Override
        public String toString() {
            return String.format("%s-%s", nodeName, address);
        }
    }
}
