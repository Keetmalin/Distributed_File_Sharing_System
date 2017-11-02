/* 
 * <Paste your header here>
 */
package org.uom.cse.distributed.peer.api;

import org.uom.cse.distributed.peer.Node;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This is the interface to be used to communicate with nodes in the network. Provides communication between nodes in
 * the distributed system.
 *
 * @author Imesha Sudasingha
 */
public abstract class CommunicationProvider {

    public abstract void start(Node node);

    public abstract void stop();

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
     * This is the method to be called iteratively on order to broadcast about the joining of new node (me) to all the
     * nodes in the network.
     *
     * @param peer   Node to which I'm notifying my presence
     * @param me     My ip and port info
     * @param nodeId My node ID. This is used to determine what are the keywords that I'm going to look at.
     * @return Map of keyword and node mappings which should be undertaken by me from this point onwards.
     */
    public abstract Map<Character, Map<String, List<EntryTableEntry>>> notifyNewNode(InetSocketAddress peer,
            InetSocketAddress me, int nodeId);

    /**
     * Sends the given file and keyword to the node given by {@link InetSocketAddress} to be indexed.
     *
     * @param peer    Owner who indexes that keyword
     * @param keyword keyword to be send
     * @param node    name of node that contains the file
     * @param file    file name to be sent
     */
    public abstract boolean offerFile(InetSocketAddress peer, String keyword, int node, String file);

    /**
     * This will search for a file name in the entire system and return the list of nodes {@link InetSocketAddress}
     *
     * @param targetNode {@link InetSocketAddress} the node that needs to be searched
     * @param fileName   the name of the file that needs to be searched
     * @param keyword    keyword to be send
     * @return Return a set of {@link InetSocketAddress}
     */
    public abstract Set<InetSocketAddress> searchFullFile(InetSocketAddress targetNode, String fileName, String keyword);

    /**
     * Pings the given node and returns the {@link EntryTable#entries} of that node.
     *
     * @param peer peer to be pinged
     * @return null if the peer couldn't be found | else entries
     */
    public abstract Map<Character, Map<String, List<EntryTableEntry>>> ping(InetSocketAddress peer,
            Map<Character, Map<String, List<EntryTableEntry>>> toBeHandedOver);
}
