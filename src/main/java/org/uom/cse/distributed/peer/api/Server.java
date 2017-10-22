package org.uom.cse.distributed.peer.api;

import org.uom.cse.distributed.peer.Node;

import java.net.DatagramPacket;

/**
 * This interface provides the node's server side representation
 *
 * @author Keet Sugathadasa
 */

public interface Server {

    void start(Node node);

    void stop();

    void listen();

    void provideRoutingTable(DatagramPacket incoming);

    void handleBroadcastRequest(String nodeName, DatagramPacket datagramPacket, String ipAddress, int port);
}
