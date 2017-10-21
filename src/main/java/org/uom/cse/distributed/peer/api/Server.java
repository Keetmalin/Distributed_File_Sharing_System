package org.uom.cse.distributed.peer.api;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

/**
 * This interface provides the node's server side representation
 *
 * @author Keet Sugathadasa
 */

public interface Server {

    void listen();
    void provideRoutingTable(DatagramPacket incoming , DatagramSocket datagramSocket);
}
