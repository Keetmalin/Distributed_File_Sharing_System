package org.uom.cse.distributed.peer;

/**
 * This class represents the entry to be stored inside the entry table
 *
 * @author Keet Sugathadasa
 */
public class EntryTableEntry {

    private String word;
    private String nodeName;

    public EntryTableEntry(String word, String nodeName) {
        if (word == null || nodeName == null) {
            throw new IllegalArgumentException("Word and Node name should not be null");
        }

        this.word = word;
        this.nodeName = nodeName;
    }

    public String getWord() {
        return word;
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
                o instanceof EntryTableEntry &&
                this.getNodeName().equals(((EntryTableEntry) o).getNodeName()) &&
                this.getWord().equals(((EntryTableEntry) o).getWord());
    }

    @Override
    public String toString() {
        return String.format("%s-%s", nodeName, word);
    }
}
