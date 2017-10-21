package org.uom.cse.distributed.peer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uom.cse.distributed.peer.api.CommunicationProvider;
import org.uom.cse.distributed.peer.utils.RequestUtils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.uom.cse.distributed.Constants.NEWNODE_MSG_FORMAT;
import static org.uom.cse.distributed.Constants.RETRIES_COUNT;

/**
 * Provides UDP Socket Based communication with Peers
 *
 * @author Keet Sugathadasa
 */
public class UDPCommunicationProvider extends CommunicationProvider {

    private static final Logger logger = LoggerFactory.getLogger(Node.class);
    private final int numOfRetries = RETRIES_COUNT;

    @Override
    public Set<RoutingTableEntry> connect(InetSocketAddress peer) {

        try (DatagramSocket datagramSocket = new DatagramSocket()) {
            String msg = messageBuilder("GETTable");

            DatagramPacket datagramPacket = new DatagramPacket(msg.getBytes(), msg.length(), peer.getAddress(),
                    peer.getPort());
            datagramSocket.send(datagramPacket);
            logger.debug("Request sent to Peer node: {}", msg);

            //start listening to Bootstrap Server Response
            byte[] buffer = new byte[65536];
            DatagramPacket incoming = new DatagramPacket(buffer, buffer.length);
            datagramSocket.receive(incoming);

            String responseMsg = new String(incoming.getData(), 0, incoming.getLength());
            logger.debug(responseMsg);
            datagramSocket.close();
        } catch (IOException e) {
            logger.error("Error occurred when connecting to node : {}", peer);
        }

        //build message to send to peer

        logger.debug("connecting to peer");
        return null;
    }

    @Override
    public boolean disconnect(InetSocketAddress peer) {
        return false;
    }

    @Override
    public Map<String, Map<String, List<Integer>>> notifyNewNode(InetSocketAddress peer, InetSocketAddress me, int nodeId) {
        String request = String.format(NEWNODE_MSG_FORMAT, me.getHostName(), me.getPort(), nodeId);
        logger.debug("Notifying new node to {} as message: {}", peer, request);

        int retriesLeft = numOfRetries;
        while (retriesLeft > 0) {
            try (DatagramSocket datagramSocket = new DatagramSocket()) {
                String response = RequestUtils.sendRequest(datagramSocket, request, peer.getAddress(), peer.getPort());
                logger.debug("Response received : {}", response);
            } catch (IOException e) {
                logger.error("Error occurred when sending the request", e);
                retriesLeft--;
            }
        }
        return null;
    }

    @Override
    public void offerFile(InetSocketAddress peer, String keyword, String file) {

    }

    private String messageBuilder(String request) {
        return request;
    }
}
