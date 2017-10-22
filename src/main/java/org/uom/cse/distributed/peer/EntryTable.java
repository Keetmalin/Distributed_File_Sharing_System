package org.uom.cse.distributed.peer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents the entry table of a given node. Each entry wil have 2 items;
 * <pre>
 *     <ul>
 *         <li>Word - the words in the file names</li>
 *         <li>Node Name - This is a number which indicates the location in the DHT</li>
 *     </ul>
 * </pre>
 *
 * @author Keet Sugathadasa
 */
public class EntryTable {

    private static final Logger logger = LoggerFactory.getLogger(RoutingTable.class);

    private final Set<EntryTableEntry> entries = Collections.synchronizedSet(new HashSet<>());

    public Set<EntryTableEntry> getEntries() {
        return entries;
    }

    public void addEntry(EntryTableEntry entryTableEntry) {
        //TODO check for duplicate entries
        List<EntryTableEntry> duplicates = this.entries.stream()
                .filter(e -> e.getWord().equals(entryTableEntry.getWord()))
                .collect(Collectors.toList());

        if (duplicates.size() == 0) {
            this.entries.add(entryTableEntry);
        } else if (duplicates.stream().filter(e -> e.getNodeName().equals(entryTableEntry.getNodeName())).count() == 1) {
            logger.warn("EntryTableEntry : {} already exists", entryTableEntry);
        } else {
            // We have an erroneous EntryTableEntry. Correct it.
            EntryTableEntry e = duplicates.get(0);
            logger.warn("Correcting entryTableEntry {} to {}", e, entryTableEntry);
            e.setNodeName(entryTableEntry.getNodeName());
        }
    }

    public boolean removeEntry(EntryTableEntry e) {
        return this.entries.remove(e);
    }

    /**
     * Finds the {@link InetSocketAddress} of a given node. Searched by the {@link EntryTableEntry#nodeName}
     *
     * @param nodeName Name of the node of which IP-port info is required to be found
     * @return Optional of {@link InetSocketAddress}
     */
    public Optional<EntryTableEntry> findByNodeName(String nodeName) {
        return this.entries.stream()
                .filter(e -> e.getNodeName().equals(nodeName))
                .findFirst();
    }
}
