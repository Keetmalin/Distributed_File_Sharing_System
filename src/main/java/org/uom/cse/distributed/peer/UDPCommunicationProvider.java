package org.uom.cse.distributed.peer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uom.cse.distributed.peer.api.CommunicationProvider;

import java.io.IOException;
import java.net.*;
import java.util.List;

/**
 * Provides UDP Socket Based communication with Peers
 *
 * @author Keet Sugathadasa
 */
public class UDPCommunicationProvider extends CommunicationProvider{

    private static final Logger logger = LoggerFactory.getLogger(Node.class);
    private final Node node;

    public UDPCommunicationProvider(Node node){
        this.node= node;
    }

    @Override
    public List<RoutingTable.Entry> connect(InetSocketAddress peer) {

        try {

            String msg = messageBuilder("GETTable");

            DatagramPacket datagramPacket = new DatagramPacket(msg.getBytes(), msg.length(),
                    InetAddress.getByName(peer.getHostName()) , peer.getPort());
            DatagramSocket datagramSocket = createDatagramSocket(this.node.getPort());
            datagramSocket.send(datagramPacket);
            logger.debug("Request sent to Peer node");
            datagramSocket.close();


            //start listening to Bootstrap Server Response
            byte[] buffer = new byte[65536];
            DatagramPacket incoming = new DatagramPacket(buffer, buffer.length);
            datagramSocket.receive(incoming);

            String responseMsg = new String(incoming.getData(), 0, incoming.getLength());
            logger.debug(responseMsg);

        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


        //build message to send to peer


        logger.debug("connecting to peer");
        return null;
    }

    @Override
    public boolean disconnect(InetSocketAddress peer) {
        return false;
    }

    private String messageBuilder(String request){
        return  request;
    }

    private DatagramSocket createDatagramSocket(int udpPort) throws SocketException {
        logger.debug("Creating Datagram Socket at port : {}", udpPort);
        return new DatagramSocket(udpPort);
    }


}
