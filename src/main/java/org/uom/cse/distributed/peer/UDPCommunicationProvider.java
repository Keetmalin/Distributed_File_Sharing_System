package org.uom.cse.distributed.peer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uom.cse.distributed.peer.api.CommunicationProvider;
import org.uom.cse.distributed.peer.utils.RequestUtils;

import java.io.IOException;
import java.net.*;
import java.util.Random;
import java.util.Set;

import static org.uom.cse.distributed.Constants.NEW_NODE_ENTRY;
import static org.uom.cse.distributed.Constants.RETRIES_COUNT;

/**
 * Provides UDP Socket Based communication with Peers
 *
 * @author Keet Sugathadasa
 */
public class UDPCommunicationProvider extends CommunicationProvider{

    private static final Logger logger = LoggerFactory.getLogger(Node.class);
    private final Node node;
    private final int numOfRetries = RETRIES_COUNT;

    public UDPCommunicationProvider(Node node){
        this.node= node;
    }

    @Override
    public Set<RoutingTableEntry> connect(InetSocketAddress peer) {

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

    private DatagramSocket createDatagramSocket() throws SocketException {
        int port = this.node.getPort() + new Random().nextInt(55536);
        logger.debug("Creating Datagram Socket at port : {}", port);
        return new DatagramSocket(port);
    }

    public void broadcast(String nodeName , DatagramPacket datagramPacket) {

        String request = buildNewNodeEntry();

        Set<RoutingTableEntry> routingEntries = node.getRoutingTable().getEntries();
        for (RoutingTableEntry routingTableEntry : routingEntries) {

            int retriesLeft = numOfRetries;
            while (retriesLeft > 0) {
                try (DatagramSocket datagramSocket = createDatagramSocket()) {
                    String response = RequestUtils.sendRequest(datagramSocket, request, routingTableEntry.getAddress().getAddress(),
                            routingTableEntry.getAddress().getPort());
                    logger.debug("Response received : {}", response);
                } catch (IOException e) {
                    logger.error("Error occurred when sending the request", e);
                    retriesLeft--;
                }
            }

        }

        logger.debug("broadcast to all entries in the routing table complete");
    }

    private String buildNewNodeEntry(){
        return NEW_NODE_ENTRY + " " + node.getUsername() + " " + node.getIpAddress() + " " + String.valueOf(node.getPort());
    }
}
