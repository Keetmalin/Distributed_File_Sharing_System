package org.uom.cse.distributed.peer.api;

import org.uom.cse.distributed.peer.Node;

import java.net.DatagramPacket;
import java.net.InetSocketAddress;

/**
 * This is the interface to be used to listen to node requests in the network. Provides server methods for nodes in
 * the distributed system
 *
 * @author Keet Sugathadasa
 */

public interface Server {

    /**
     * this starts a server on the node that is capable of listening to incoming requests
     *
     * @param node {@link Node} the node which needs to instantiate the server side
     */
    void start(Node node);

    /**
     * this stops a server on the node that is capable of listening to incoming request
     */
    void stop();

    /**
     * this starts listening to incoming packets from other nodes
     */
    void listen();

    /**
     * this gets the current nodes routing table and pass it down to whoever is requesting is
     *
     * @param incoming {@link DatagramPacket} the incoming packet handed over with the request
     */
    void provideRoutingTable(DatagramPacket incoming);

    /**
     * this gets the current nodes routing table and pass it down to whoever is requesting is
     *
     * @param nodeName name of the nodeID of the new node that sent the request
     * @param datagramPacket {@link DatagramPacket} the incoming packet handed over with the request
     * @param ipAddress IP Address of the node that sent he request
     * @param port port number of the node that sent the request
     */
    void handleBroadcastRequest(String nodeName, DatagramPacket datagramPacket, String ipAddress, int port);
}
