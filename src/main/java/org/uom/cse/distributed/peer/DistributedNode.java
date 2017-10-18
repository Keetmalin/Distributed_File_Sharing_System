package org.uom.cse.distributed.peer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uom.cse.distributed.peer.api.BootstrapProvider;
import org.uom.cse.distributed.peer.api.State;
import org.uom.cse.distributed.peer.api.StateManager;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.uom.cse.distributed.peer.api.State.IDLE;
import static org.uom.cse.distributed.peer.api.State.REGISTERED;

/**
 * The class to represent a Node in the distributed network.
 *
 * @author Keet Malin
 * @author Imesha Sudasingha
 */
public class DistributedNode {

    private static final Logger logger = LoggerFactory.getLogger(DistributedNode.class);

    private final StateManager stateManager = new StateManager(IDLE);
    private final List<InetSocketAddress> peers = new ArrayList<>();
    private int port;
    private String username;
    private String ipAddress;

    private BootstrapProvider bootstrapProvider = new UDPBootstrapProvider();

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
        stateManager.checkState(State.IDLE);

        logger.debug("Registering node");
        try {
            List<InetSocketAddress> peers = bootstrapProvider.register(ipAddress, port, username);
            if (peers != null) {
                this.peers.addAll(peers);
            } else {
                logger.warn("Peers are null");
            }
        } catch (IOException e) {
            logger.error("Error occurred when registering node", e);
            throw new IllegalStateException("Unable to register this node", e);
        }

        stateManager.setState(REGISTERED);
        logger.info("Node registered successfully, Program started at {}:{}", ipAddress, port);
    }


    public void stop() {
        logger.debug("Stopping node");
        if (stateManager.getState().compareTo(REGISTERED) >= 0) {
            peers.clear();
            try {
                bootstrapProvider.unregister(ipAddress, port, username);
            } catch (IOException e) {
                logger.error("Error occurred when unregistering", e);
            }
        }

        stateManager.setState(IDLE);
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

    @Override
    public void finalize() throws Throwable {
        stop();
        super.finalize();
    }
}