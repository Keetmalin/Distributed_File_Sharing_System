package org.uom.cse.distributed.peer.api;

import java.io.Serializable;

/**
 * This class represents the entry to be stored inside the entry table
 *
 * @author Keet Sugathadasa
 */
public class EntryTableEntry implements Serializable {

    private String file;
    private String nodeName;

    public EntryTableEntry() { }

    public EntryTableEntry(String nodeName, String file) {
        if (nodeName == null || file == null) {
            throw new IllegalArgumentException("Node name and File should not be null");
        }

        this.nodeName = nodeName;
        this.file = file;
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

    public void setFile(String file) {
        this.file = file;
    }

    @Override
    public boolean equals(Object o) {
        return o != null &&
                o instanceof EntryTableEntry &&
                this.getNodeName().equals(((EntryTableEntry) o).getNodeName()) &&
                this.getFileName().equals(((EntryTableEntry) o).getFileName());
    }

    @Override
    public String toString() {
        return String.format("[%s -> %s]", nodeName, file);
    }
}
