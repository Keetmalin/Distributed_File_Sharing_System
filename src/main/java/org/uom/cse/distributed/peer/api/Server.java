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

    void stop();

    void listen();

    void provideRoutingTable(DatagramPacket incoming);

    void handleBroadcastRequest(String nodeName, DatagramPacket datagramPacket, String ipAddress, int port);
}
