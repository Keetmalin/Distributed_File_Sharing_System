package org.uom.cse.distributed.peer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uom.cse.distributed.peer.api.BootstrapProvider;
import org.uom.cse.distributed.peer.api.CommunicationProvider;
import org.uom.cse.distributed.peer.api.State;
import org.uom.cse.distributed.peer.api.StateManager;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.uom.cse.distributed.peer.api.State.CONNECTED;
import static org.uom.cse.distributed.peer.api.State.IDLE;
import static org.uom.cse.distributed.peer.api.State.REGISTERED;

/**
 * The class to represent a Node in the distributed network.
 *
 * @author Keet Malin
 * @author Imesha Sudasingha
 */
public class Node {

    private static final Logger logger = LoggerFactory.getLogger(Node.class);

    private final StateManager stateManager = new StateManager(IDLE);
    private final List<InetSocketAddress> peers = new ArrayList<>();
    private final RoutingTable routingTable = new RoutingTable();
    private final CommunicationProvider communicationProvider;
    private final String username;
    private final String ipAddress;
    private final int port;

    private BootstrapProvider bootstrapProvider = new UDPBootstrapProvider();

    public Node(int port, CommunicationProvider communicationProvider) {
        this(port, "localhost", communicationProvider);
    }

    public Node(int port, String ipAddress, CommunicationProvider communicationProvider) {
        this(port, ipAddress, UUID.randomUUID().toString(), communicationProvider);
    }

    public Node(int port, String ipAddress, String username, CommunicationProvider communicationProvider) {
        this.port = port;
        this.ipAddress = ipAddress;
        this.username = username;
        this.communicationProvider = communicationProvider;
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
        logger.info("Node registered successfully", ipAddress, port);

        this.peers.forEach(peer -> {
            List<RoutingTable.Entry> entries = communicationProvider.connect(peer);
            entries.forEach(routingTable::addEntry);
        });
        stateManager.setState(CONNECTED);
        logger.info("Successfully connected to the network and created routing table");
    }

    public void stop() {
        logger.debug("Stopping node");
        if (stateManager.getState().compareTo(REGISTERED) >= 0) {

            if (stateManager.getState().compareTo(CONNECTED) >= 0) {
                // TODO: 10/20/17 Should we disconnect from the peers or all entries in the routing table?
                this.peers.forEach(peer -> {
                    if (communicationProvider.disconnect(peer)) {
                        logger.debug("Successfully disconnected from {}", peer);
                    } else {
                        logger.warn("Unable to disconnect from {}", peer);
                    }
                });

                stateManager.setState(REGISTERED);
            }

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