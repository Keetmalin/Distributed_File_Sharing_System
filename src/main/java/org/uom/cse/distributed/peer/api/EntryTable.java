package org.uom.cse.distributed.peer.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uom.cse.distributed.peer.RoutingTable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents the entry table of a given node. Entries are kept as
 * <pre>
 *     letter -> {
 *          keyword1 -> [{node1, file1}, {node2, file2},...],
 *          keyword2 -> [{node1, file1}, {node2, file2},...]
 *          keyword3 -> [{node1, file1}, {node2, file2},...]
 *     }
 * </pre>
 *
 * @author Imesha Sudasingha
 * @author Keet Sugathadasa
 */
public class EntryTable {

    private static final Logger logger = LoggerFactory.getLogger(RoutingTable.class);

    private final Map<Character, Map<String, List<EntryTableEntry>>> entries = new ConcurrentHashMap<>();

    public void addCharacter(Character c) {
        logger.info("Adding character {} to my entry table", c);
        entries.putIfAbsent(c, new HashMap<>());
    }

    public void addEntry(String keyword, EntryTableEntry entry) {
        if (keyword == null || entry == null) {
            throw new IllegalArgumentException("Keyword and entry cannot be null");
        }

        char c = keyword.charAt(0);

        if (!entries.containsKey(c)) {
            throw new IllegalArgumentException("Character " + c + " is not in my entries");
        }

        logger.debug("Adding entry {} to entry table", entry);
        if (entries.get(c).containsKey(keyword)) {
            if (entries.get(c).get(keyword).contains(entry)) {
                logger.warn("{} already exists", entry);
            } else {
                logger.info("Adding entry {} to entry table", entry);
                entries.get(c).get(keyword).add(entry);
            }
        } else {
            List<EntryTableEntry> list = new ArrayList<>();
            list.add(entry);
            logger.info("Adding keyword: {} and entry: {} to entry table", keyword, entry);
            entries.get(c).put(keyword, list);
        }
    }

    public boolean removeEntry(String keyword, EntryTableEntry entry) {
        logger.debug("Removing entry {}->{}", keyword, entry);
        return keyword != null &&
                entries.containsKey(keyword.charAt(0)) &&
                this.entries.get(keyword.charAt(0)).containsKey(keyword) &&
                this.entries.get(keyword.charAt(0)).get(keyword).remove(entry);

    }

    public void clear() {
        entries.clear();
        logger.info("Cleared entry table");
    }

    public Map<Character, Map<String, List<EntryTableEntry>>> getEntries() {
        return entries;
    }
}
