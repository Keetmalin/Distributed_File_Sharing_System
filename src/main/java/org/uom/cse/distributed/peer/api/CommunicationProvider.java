/* 
 * <Paste your header here>
 */
package org.uom.cse.distributed.peer.api;

import org.uom.cse.distributed.peer.RoutingTableEntry;

import java.net.InetSocketAddress;
import java.util.Set;

/**
 * This is the interface to be used to communicate with nodes in the network. Provides communication between nodes in
 * the distributed system.
 *
 * @author Imesha Sudasingha
 */
public abstract class CommunicationProvider {

    /**
     * Connects to the peer given by the IP and port. The peer will (hopefully) return the routing table of that node.
     * This method will return the list of routing table entries received from the peer.
     *
     * @param peer {@link InetSocketAddress} of the peer we are connecting
     * @return List of routing table entries of the peer.
     */
    public abstract Set<RoutingTableEntry> connect(InetSocketAddress peer);

    /**
     * Disconnects from the given peer after notifying that node that I'm disconnecting.
     *
     * @param peer peer from which I'm going to disconnect from
     */
    public abstract boolean disconnect(InetSocketAddress peer);

    /**
     * Connect to a peer and retrieve its routing table to client side
     *
     * @return void
     */
    public abstract void connectToPeer();
}
