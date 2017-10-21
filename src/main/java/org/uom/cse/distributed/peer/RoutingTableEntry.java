package org.uom.cse.distributed.peer;

import java.net.InetSocketAddress;

/**
 * This class represents the entry to be stored inside the routing table
 *
 * @author Keet Sugathadasa
 */

public class RoutingTableEntry {
    private InetSocketAddress address;
    private String nodeName;

    public RoutingTableEntry(InetSocketAddress address, String nodeName) {
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
                o instanceof RoutingTableEntry &&
                this.getNodeName().equals(((RoutingTableEntry) o).getNodeName()) &&
                this.getAddress().equals(((RoutingTableEntry) o).getAddress());
    }

    @Override
    public String toString() {
        return String.format("%s-%s", nodeName, address);
    }
}
