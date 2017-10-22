package org.uom.cse.distributed.peer;

import java.io.Serializable;

/**
 * This class represents the entry to be stored inside the entry table
 *
 * @author Keet Sugathadasa
 */
public class EntryTableEntry implements Serializable{

    private String word;
    private String file;
    private String nodeName;

    public EntryTableEntry(String word, String nodeName, String file) {
        if (word == null || nodeName == null || file == null) {
            throw new IllegalArgumentException("Word, Node name and File should not be null");
        }

        this.word = word;
        this.nodeName = nodeName;
        this.file = file;
    }

    public String getWord() {
        return word;
    }

    public String getNodeName() {
        return nodeName;
    }

    public String getFileName() {
        return file;
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
