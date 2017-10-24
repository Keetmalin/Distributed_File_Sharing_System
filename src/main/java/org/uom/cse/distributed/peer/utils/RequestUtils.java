/* 
 * <Paste your header here>
 */
package org.uom.cse.distributed.peer.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uom.cse.distributed.Constants;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.StringTokenizer;

/**
 * A utility class to send requests to destinations/peers.
 *
 * @author Imesha Sudasingha
 */
public class RequestUtils {

    private static final Logger logger = LoggerFactory.getLogger(RequestUtils.class);

    private RequestUtils() { }

    /**
     * Sends the UDP request through the given {@link DatagramSocket}. This is a blocking method call.
     *
     * @param datagramSocket datagram socket
     * @throws IOException sending failures
     */
    public static String sendRequest(DatagramSocket datagramSocket, String request,
            InetAddress address, int port) throws IOException {
        logger.debug("Sending request: '{}' to bootstrap server", request);

        // Create a datagram packet to send to the Boostrap server
        DatagramPacket datagramPacket = new DatagramPacket(request.getBytes(), request.length(), address, port);
        // Send to bootstrap server
        datagramSocket.send(datagramPacket);
        logger.debug("Datagram packet sent, listening for response", request);
        // Start listening to Bootstrap Server Response. First 4 bytes are read first to identify length of message.
        byte[] buffer = new byte[65536];
        DatagramPacket incoming = new DatagramPacket(buffer, buffer.length);
        datagramSocket.receive(incoming);

        return new String(incoming.getData(), 0, incoming.getLength());
    }

    /**
     * Sends the UDP request through the given {@link DatagramSocket}. This is a blocking method call.
     *
     * @param datagramSocket datagram socket
     * @throws IOException sending failures
     */
    public static void sendObjectRequest(DatagramSocket datagramSocket, Object requestObject,
            InetAddress address, int port) throws IOException {
        logger.debug("Sending request Object to recipient {}:{}", address, port);

        //create a Byte Stream out of the object
        ByteArrayOutputStream baos = new ByteArrayOutputStream(6400);
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(requestObject);
        String base64 = Base64.getEncoder().encodeToString(baos.toByteArray());
        byte[] data = base64.getBytes();

        // Create a datagram packet to send to the recipient
        DatagramPacket datagramPacket = new DatagramPacket(data, data.length, address, port);
        // Send to recipient
        datagramSocket.send(datagramPacket);
        logger.debug("Datagram packet sent to recipient {}:{}", address, port);
    }


    /**
     * Sends the UDP request through the given {@link DatagramSocket}. This is a blocking method call.
     *
     * @param datagramSocket datagram socket
     * @throws IOException sending failures
     */
    public static void sendResponse(DatagramSocket datagramSocket, String response,
            InetAddress address, int port) throws IOException {
        logger.debug("Sending response to recipient");

        // Create a datagram packet to send to the recipient
        DatagramPacket datagramPacket = new DatagramPacket(response.getBytes(), response.length(), address, port);
        // Send to recipient
        datagramSocket.send(datagramPacket);
        logger.debug("Datagram packet sent, listening for response", response);

    }

    /**
     * Process the response received from the Bootstrap server in order to parse the peer information received.
     *
     * @param response response received from the bootstrap server
     * @return list of peers received
     */
    public static List<InetSocketAddress> processRegisterResponse(String response) {
        logger.debug("Processing response : {}", response);

        StringTokenizer st = new StringTokenizer(response, " ");
        logger.debug("Response length: {}", st.nextToken());
        String status = st.nextToken();

        if (!Constants.REGOK.equals(status)) {
            throw new IllegalStateException(Constants.REGOK + " not received");
        }

        int code = Integer.parseInt(st.nextToken());

        List<InetSocketAddress> peers = new ArrayList<>();

        switch (code) {
            case 0:
                logger.info("Successful - No nodes in the network yet");
                break;
            case 1:
            case 2:
                logger.info("Successful - Found 1/2 other nodes in the network");
                while (st.hasMoreTokens()) {
                    peers.add(new InetSocketAddress(st.nextToken(), Integer.parseInt(st.nextToken())));
                }
                break;
            case 9999:
                logger.error("Failed. There are errors in your command");
                break;
            case 9998:
                logger.error("Failed, already registered to you, unregister first");
                break;
            case 9997:
                logger.error("Failed, registered to another user, try a different IP and port");
                break;
            case 9996:
                logger.error("Failed, can’t register. BS full.");
                break;
            default:
                throw new IllegalStateException("No proper status code returned");
        }

        return peers;
    }

    /**
     * Processes the unregister request's response coming from the server
     *
     * @param response response received
     * @return true if successful
     */
    public static boolean processUnregisterResponse(String response) {
        logger.debug("Processing unregister response : {}", response);

        StringTokenizer st = new StringTokenizer(response, " ");
        logger.debug("Response length: {}", st.nextToken());
        String status = st.nextToken();

        if (!Constants.UNROK.equals(status)) {
            throw new IllegalStateException(Constants.UNROK + " not received");
        }

        int code = Integer.parseInt(st.nextToken());

        List<InetSocketAddress> peers = new ArrayList<>();

        switch (code) {
            case 0:
                logger.info("Successful");
                return true;
            case 9999:
                logger.error("Error while un-registering. IP and port may not be in the registry or command is incorrect");
            default:
                return false;
        }
    }

    /**
     * Builds the message to the format <pre>%4d %s</pre> which will be the actual request sent to the Bootstrap server
     *
     * @param request Request to be included in the message
     * @return formatted message
     */
    public static String buildRequest(String request) {
        return String.format(Constants.MSG_FORMAT, request.length() + 5, request);
    }

}
