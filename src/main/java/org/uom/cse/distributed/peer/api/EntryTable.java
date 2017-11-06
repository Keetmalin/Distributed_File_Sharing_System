package org.uom.cse.distributed.peer.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private static final Logger logger = LoggerFactory.getLogger(EntryTable.class);

    private final Map<Character, Map<String, List<EntryTableEntry>>> entries = new HashMap<>();

    public synchronized void addCharacter(Character c) {
        c = Character.toUpperCase(c);
        logger.info("Adding character [{}] to my entry table", c);
        entries.putIfAbsent(c, new HashMap<>());
    }

    public synchronized void addEntry(String keyword, EntryTableEntry entry) {
        if (keyword == null || entry == null) {
            throw new IllegalArgumentException("Keyword and entry cannot be null");
        }

        char c = keyword.toUpperCase().charAt(0);

        if (!entries.containsKey(c)) {
            logger.warn("[{}] not in my characters. But adding for consistency", c);
            addCharacter(c);
        }

        logger.debug("Adding entry -> {}", entry);
        if (entries.get(c).containsKey(keyword)) {
            if (entries.get(c).get(keyword).contains(entry)) {
                logger.warn("{} already exists", entry);
            } else {
                entries.get(c).get(keyword).add(entry);
                logger.info("Added entry -> {}", entry);
            }
        } else {
            List<EntryTableEntry> list = new ArrayList<>();
            list.add(entry);
            logger.info("Adding keyword [{}] and entry -> {}", keyword, entry);
            entries.get(c).put(keyword, list);
        }
    }

    public synchronized boolean removeEntry(String keyword, EntryTableEntry entry) {
        logger.debug("Removing entry {}->{}", keyword, entry);
        if (keyword != null) {
            char c = keyword.toUpperCase().charAt(0);
            return entries.containsKey(c) &&
                    this.entries.get(c).containsKey(keyword) &&
                    this.entries.get(c).get(keyword).remove(entry);
        }
        return false;
    }

    /**
     * Removes all the mappings for the given character
     *
     * @param c character to be removed
     * @return true if the character was in our entries
     */
    public synchronized boolean removeCharacter(char c) {
        c = Character.toUpperCase(c);
        if (entries.containsKey(c)) {
            logger.info("Removing character [{}] from entries", c);
            entries.remove(c);
        }

        return false;
    }

    public synchronized void addAll(Map<Character, Map<String, List<EntryTableEntry>>> table) {
        if (table == null) return;

        table.forEach((character, keywordMap) -> keywordMap.forEach((keyword, entries) -> {
            entries.forEach(entry -> this.addEntry(keyword, entry));
        }));
    }

    /**
     * Gets all the keywords for a given character
     *
     * @param c character
     * @return keywords under that character
     */
    public synchronized Map<String, List<EntryTableEntry>> getKeywordsFor(char c) {
        c = Character.toUpperCase(c);
        return entries.get(c);
    }

    public synchronized void clear() {
        entries.clear();
        logger.info("Cleared entry table");
    }

    public synchronized Map<Character, Map<String, List<EntryTableEntry>>> getEntries() {
        return entries;
    }

    public synchronized List<EntryTableEntry> getEntriesByKyeword(String key) {
        return getKeywordsFor(Character.toUpperCase(key.charAt(0))).get(key);
    }
}
