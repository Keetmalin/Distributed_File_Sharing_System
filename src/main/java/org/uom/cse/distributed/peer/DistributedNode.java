package org.uom.cse.distributed.peer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uom.cse.distributed.Constants;
import org.uom.cse.distributed.peer.api.State;
import org.uom.cse.distributed.peer.api.StateManager;
import org.uom.cse.distributed.peer.utils.RequestUtils;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.uom.cse.distributed.Constants.RETRIES_COUNT;
import static org.uom.cse.distributed.peer.api.State.CONNECTED;
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
        if (stateManager.getState().compareTo(STARTING) <= 0) {
            stateManager.setState(STARTING);
            logger.debug("Starting program on port - {}, ip - {}", port, ipAddress);

            logger.debug("Creating Datagram Socket at port : {}", port);
            try {
                datagramSocket = new DatagramSocket(port);
                logger.info("Datagram Socket created at port : {}", port);
                stateManager.setState(STARTED);
            } catch (SocketException e) {
                logger.error("Error when creating datagram socket", e);
                throw new IllegalStateException("Unable to create datagram socket", e);
            }
        }

        logger.debug("Registering node");
        register();
        stateManager.setState(REGISTERED);
        logger.info("Node registered successfully");

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
        String request = String.format(Constants.MSG_FORMAT, msg.length(), msg);

        int retriesLeft = numOfRetries;
        while (retriesLeft > 0) {
            try {
                String response = RequestUtils.sendRequest(datagramSocket, request,
                        InetAddress.getByName(Constants.BOOTSTRAP_IP), Constants.BOOTSTRAP_PORT);
                logger.debug("Response received : {}", response);
                List<InetSocketAddress> peers = RequestUtils.processRegisterResponse(response);
                logger.debug("Response processed successfully. Found peers : {}", peers);
                this.peers.addAll(peers);
                break;
            } catch (IOException e) {
                logger.error("Error occurred when sending the request", e);
                retriesLeft--;
            }
        }
    }

    private void unregister() {
        stateManager.checkState(REGISTERED, CONNECTED);
        logger.debug("Unregistering node");

        String msg = String.format(Constants.UNREG_MSG_FORMAT, ipAddress, port, username);
        String request = String.format(Constants.MSG_FORMAT, msg.length(), msg);

        int retriesLeft = numOfRetries;
        while (retriesLeft > 0) {
            try {
                String response = RequestUtils.sendRequest(datagramSocket, request,
                        InetAddress.getByName(Constants.BOOTSTRAP_IP), Constants.BOOTSTRAP_PORT);
                logger.debug("Response received : {}", response);
                if (RequestUtils.processUnregisterResponse(response)) {
                    logger.info("Successfully unregistered");
                    stateManager.setState(STARTED);
                } else {
                    logger.warn("Unable to unregister");
                }
                break;
            } catch (IOException e) {
                logger.error("Error occurred when sending the request", e);
                retriesLeft--;
            }
        }
    }

    public void stop() {
        logger.debug("Stopping node");
        if (stateManager.getState().compareTo(REGISTERED) >= 0) {
            peers.clear();
            unregister();
        }

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

    public List<InetSocketAddress> getPeers() {
        return peers;
    }

    public State getState() {
        return stateManager.getState();
    }

    public DatagramSocket getDatagramSocket() {
        return datagramSocket;
    }

    @Override
    public void finalize() throws Throwable {
        stop();
        super.finalize();
    }
}