package org.uom.cse.distributed.peer;

import org.uom.cse.distributed.peer.api.Server;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

/**
 * This Class provides the implementation of the server side, of each
 * of the nodes.
 *
 * @author Vithusha Aarabhi
 * @author Jayan Vidanapathirana
 */
public class RestServer implements Server{

    @Override
    public void listen() {

    }

    @Override
    public void provideRoutingTable(DatagramPacket incoming, DatagramSocket datagramSocket) {

    }
}
