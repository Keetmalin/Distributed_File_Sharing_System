package org.uom.cse.distributed.peer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uom.cse.distributed.Constants;
import org.uom.cse.distributed.peer.api.StateManager;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.UUID;

import static org.uom.cse.distributed.Constants.BOOTSTRAP_IP;
import static org.uom.cse.distributed.Constants.BOOTSTRAP_PORT;
import static org.uom.cse.distributed.Constants.RETRIES_COUNT;
import static org.uom.cse.distributed.peer.api.State.REGISTERED;
import static org.uom.cse.distributed.peer.api.State.STARTED;
import static org.uom.cse.distributed.peer.api.State.STARTING;
import static org.uom.cse.distributed.peer.api.State.STOPPED;

/**
 * The class to represent a Node in the distributed network.
 *
 * @author Keet Malin
 * @author Imesha Sudasingha
 */
public class DistributedNode {

    private static final Logger logger = LoggerFactory.getLogger(DistributedNode.class);

    private final int numOfRetries = RETRIES_COUNT;
    private final StateManager stateManager = new StateManager(STOPPED);
    private final List<InetSocketAddress> peers = new ArrayList<>();
    private int port;
    private String username;
    private String ipAddress;
    private DatagramSocket datagramSocket;

    public DistributedNode(int port) {
        this(port, "localhost");
    }

    public DistributedNode(int port, String ipAddress) {
        this(port, ipAddress, UUID.randomUUID().toString());
    }

    public DistributedNode(int port, String ipAddress, String username) {
        this.port = port;
        this.ipAddress = ipAddress;
        this.username = username;
    }

    public void start() {
        stateManager.checkState(STOPPED, STARTING);
        stateManager.setState(STARTING);
        logger.debug("Starting program on port - {}, ip - {}", port, ipAddress);

        logger.debug("Creating Datagram Socket at port : {}", port);
        try {
            datagramSocket = new DatagramSocket(port);
            stateManager.setState(STARTED);
            logger.info("Datagram Socket created at port : {}", port);

            register();
            stateManager.setState(REGISTERED);
            logger.info("Node registered successfully");
        } catch (SocketException e) {
            logger.error("Error when creating datagram socket", e);
            throw new IllegalStateException("Unable to create datagram socket", e);
        }

        logger.info("Program started and registered on port - {}:{}", ipAddress, port);
    }

    /**
     * Registers this node in the bootstrap server. Changes {@link org.uom.cse.distributed.peer.api.State} accordingly.
     * A {@link RuntimeException} can be thrown if any error occurs.
     *
     * @throws RuntimeException due to failures
     */
    private void register() {
        stateManager.checkState(STARTED);

        String msg = String.format(Constants.REG_MSG_FORMAT, ipAddress, port, username);

        int retriesLeft = numOfRetries;
        while (retriesLeft > 0) {
            try {
                String response = sendRegisterRequest(datagramSocket, msg);
                logger.debug("Response received : {}", response);
                List<InetSocketAddress> peers = processResponse(response);
                logger.debug("Response processed successfully. Found peers : {}", peers);
                this.peers.addAll(peers);
                break;
            } catch (IOException e) {
                logger.error("Error occurred when sending the request", e);
                retriesLeft--;
            }
        }
    }

    /**
     * Sends the UDP request through the given {@link DatagramSocket}. This is a blocking method call.
     *
     * @param datagramSocket datagram socket
     * @throws IOException sending failures
     */
    private String sendRegisterRequest(DatagramSocket datagramSocket, String msg) throws IOException {
        String request = String.format(Constants.MSG_FORMAT, msg.length(), msg);

        logger.debug("Sending REG request: '{}' to bootstrap server", request);

        // Create a datagram packet to send to the Boostrap server
        DatagramPacket datagramPacket = new DatagramPacket(request.getBytes(), request.length(),
                InetAddress.getByName(BOOTSTRAP_IP), BOOTSTRAP_PORT);
        // Send to bootstrap server
        datagramSocket.send(datagramPacket);
        logger.debug("Datagram packet sent. Listening for response", request);
        // Start listening to Bootstrap Server Response. First 4 bytes are read first to identify length of message.
        byte[] buffer = new byte[65536];
        DatagramPacket incoming = new DatagramPacket(buffer, buffer.length);
        datagramSocket.receive(incoming);

        return new String(incoming.getData(), 0, incoming.getLength());
    }

    /**
     * Process the response received from the Bootstrap server in order to parse the peer information received.
     *
     * @param response response received from the bootstrap server
     * @return list of peers received
     */
    private List<InetSocketAddress> processResponse(String response) {
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

    public void stop() {
        logger.debug("Stopping node");
        if (stateManager.getState().compareTo(STARTED) >= 0) {
            logger.debug("Stopping Datagram Socket - {}:{}", ipAddress, port);
            datagramSocket.close();
            stateManager.setState(STOPPED);
        }

        logger.info("Distributed node stopped");
    }

    public int getPort() {
        return port;
    }

    public String getUsername() {
        return username;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    @Override
    public void finalize() throws Throwable {
        stop();
        super.finalize();
    }
}