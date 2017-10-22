package org.uom.cse.distributed.peer;

import org.uom.cse.distributed.peer.api.Server;

import java.net.DatagramPacket;

/**
 * This Class provides the implementation of the server side, of each of the nodes.
 *
 * @author Vithusha Aarabhi
 * @author Jayan Vidanapathirana
 */
public class RestServer implements Server {


    @Override
    public void start(Node node) {

    }

    @Override
    public void stop() {

    }

    @Override
    public void listen() {

    }

    @Override
    public void provideRoutingTable(DatagramPacket incoming) {

    }

    @Override
    public void handleBroadcastRequest(String nodeName, DatagramPacket datagramPacket, String ipAddress, int port) {

    }


}
