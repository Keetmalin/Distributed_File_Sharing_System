package org.uom.cse.distributed.peer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uom.cse.distributed.peer.api.EntryTableEntry;
import org.uom.cse.distributed.peer.api.Server;
import org.uom.cse.distributed.peer.utils.RequestUtils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.uom.cse.distributed.Constants.GET_ROUTING_TABLE;
import static org.uom.cse.distributed.Constants.NEW_ENTRY;
import static org.uom.cse.distributed.Constants.NEW_NODE;
import static org.uom.cse.distributed.Constants.QUERY;
import static org.uom.cse.distributed.Constants.RESPONSE_FAILURE;
import static org.uom.cse.distributed.Constants.RESPONSE_OK;
import static org.uom.cse.distributed.Constants.RETRIES_COUNT;
import static org.uom.cse.distributed.Constants.RETRY_TIMEOUT_MS;

/**
 * This class implements the server side listening and handling of requests Via UDP - for each node in the Distributed
 * Network
 *
 * @author Keet Sugathadasa
 * @author Imesha Sudasingha
 */
public class UDPServer implements Server {

    private static final Logger logger = LoggerFactory.getLogger(Node.class);

    private final int numOfRetries = RETRIES_COUNT;
    private ExecutorService executorService;
    private boolean started = false;
    private final int port;

    private Node node;

    public UDPServer(int port) {
        this.port = port;
    }

    @Override
    public void start(Node node) {
        if (started) {
            logger.warn("Listener already running");
            return;
        }

        this.node = node;

        executorService = Executors.newCachedThreadPool();
        executorService.submit(() -> {
            try {
                listen();
            } catch (Exception e) {
                logger.error("Error occurred when listening", e);
            }
        });

        started = true;
        logger.info("Server started");
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
    }

    @Override
    public void listen() {
        try (DatagramSocket datagramSocket = new DatagramSocket(port)) {
            logger.debug("Node is Listening to incoming requests");

            while (started) {
                byte[] buffer = new byte[65536];
                DatagramPacket incoming = new DatagramPacket(buffer, buffer.length);
                datagramSocket.receive(incoming);

                byte[] data = incoming.getData();
                String request = new String(data, 0, incoming.getLength());
                logger.debug("Received from {}:{} -> {}", incoming.getAddress(), incoming.getPort(), request);

                executorService.submit(() -> {
                    try {
                        handleRequest(request, incoming);
                    } catch (Exception e) {
                        logger.error("Error occurred when handling request ({})", request, e);
                        retryOrTimeout(RESPONSE_FAILURE, new InetSocketAddress(incoming.getAddress(), incoming.getPort()));
                    }
                });
            }
        } catch (IOException e) {
            logger.error("Error occurred when listening on port {}", port, e);
            throw new IllegalStateException("Error occurred when listening", e);
        }
    }

    private void handleRequest(String request, DatagramPacket incoming) throws IOException {
        String[] incomingResult = request.split(" ", 3);
        logger.debug("Request length -> {}", incomingResult[0]);
        String command = incomingResult[1];
        logger.debug("Command -> {}", command);

        InetSocketAddress recipient = new InetSocketAddress(incoming.getAddress(), incoming.getPort());
        switch (command) {
            case GET_ROUTING_TABLE:
                provideRoutingTable(recipient);
                break;
            case NEW_NODE:
                handleNewNodeRequest(incomingResult[2], recipient);
                break;
            case NEW_ENTRY:
                String[] list = incomingResult[2].split(" ", 3);
                logger.debug("Adding entry to entry table -> {}", list);
                this.node.getEntryTable().addEntry(list[0], new EntryTableEntry(list[1], list[2]));
                retryOrTimeout(RESPONSE_OK, recipient);
                break;
            case QUERY:
                String[] parts = incomingResult[2].split(" ");
                InetSocketAddress[] inetSocketAddresses = getNodeList(searchEntryTable(parts[0], parts[1]));
                provideAddressArray(recipient, inetSocketAddresses);
                break;
        }
    }

    @Override
    public void provideRoutingTable(InetSocketAddress recipient) throws IOException {
        logger.debug("Returning routing table to -> {}", recipient);
        String response;
        try {
            response = RequestUtils.buildObjectRequest(this.node.getRoutingTable().getEntries());
        } catch (IOException e) {
            logger.error("Error occurred when building object request: {}", e);
            throw e;
        }
        retryOrTimeout(response, recipient);
        logger.debug("Routing table entries provided to the recipient: {}", recipient);
    }

    @Override
    public void handleNewNodeRequest(String request, InetSocketAddress recipient) throws IOException {
        String[] parts = request.split(" ");
        String ipAddress = parts[0];
        int port = Integer.parseInt(parts[1]);
        int newNodeId = Integer.parseInt(parts[2]);

        this.node.addNewNode(ipAddress, port, newNodeId);
        Map<Character, Map<String, List<EntryTableEntry>>> entriesToHandover = this.node.getEntriesToHandoverTo(newNodeId);

        if (entriesToHandover == null) {
            logger.error("Couldn't find characters to be handed over to node -> {}", newNodeId);
            retryOrTimeout(RESPONSE_FAILURE, recipient);
            return;
        }

        // Send the entries to new node. Only if that's successful, we remove them from myself
        logger.debug("Notifying characters belonging to node -> {} : {}", newNodeId, entriesToHandover.keySet());
        String response;
        try {
            response = RequestUtils.buildObjectRequest(entriesToHandover);
        } catch (IOException e) {
            logger.error("Error occurred when building object request: {}", e);
            throw e;
        }

        if (retryOrTimeout(response, recipient)) {
            logger.debug("Successfully notified characters ({}) to node -> {}", entriesToHandover.keySet(), newNodeId);
            node.removeEntries(entriesToHandover.keySet());
        } else {
            logger.error("Unable to notify entries to node -> {}. Retaining chars for now", newNodeId);
            retryOrTimeout(RESPONSE_FAILURE, recipient);
        }
    }

    /**
     * This method retries a given response or times out of that response fails. Tries for maximum of {@link
     * UDPCommunicationProvider#numOfRetries}
     *
     * @param response response to be sent
     * @param peer     to whom the response is sent
     * @return true if successful | false if failed
     */
    private boolean retryOrTimeout(String response, InetSocketAddress peer) {
        int retriesLeft = numOfRetries;
        while (retriesLeft > 0) {
            Future<Void> task = executorService.submit(() -> {
                try (DatagramSocket datagramSocket = new DatagramSocket()) {
                    RequestUtils.sendResponse(datagramSocket, response, peer.getAddress(), peer.getPort());
                    return null;
                }
            });

            try {
                task.get(RETRY_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                return true;
            } catch (Exception e) {
                logger.error("Error occurred when completing response({}) to peer- {}. Error: {}", response, peer, e);
                task.cancel(true);
                retriesLeft--;
            }
        }

        logger.error("RESPONSE FAILED !!! ({} -> {})", response, peer);
        return false;
    }


    private void provideAddressArray(InetSocketAddress recipient, InetSocketAddress[] inetSocketAddresses) throws IOException {
        logger.debug("Returning addresses {} to -> {}", inetSocketAddresses, recipient);
        String response;
        try {
            response = RequestUtils.buildObjectRequest(inetSocketAddresses);
        } catch (IOException e) {
            logger.error("Error occurred when building object request: {}", e);
            throw e;
        }

        retryOrTimeout(response, recipient);
        logger.debug("Array of node addresses provided to the recipient: {}", recipient);
    }

    private List<String> searchEntryTable(String keyword, String fileName) {
        char c = keyword.charAt(0);
        List<EntryTableEntry> entryList = this.node.getEntryTable().getEntries().get(c).get(keyword);
        List<String> results = new ArrayList<String>();

        for (EntryTableEntry entry : entryList) {

            if (fileName.equals(entry.getFileName())) {
                results.add(entry.getNodeName());
            }

        }
        return results;
    }

    private InetSocketAddress[] getNodeList(List<String> nodeNameList) {
        Set<RoutingTableEntry> entries = this.node.getRoutingTable().getEntries();
        InetSocketAddress[] inetSocketAddresses = new InetSocketAddress[nodeNameList.size()];

        int i = 0;
        for (RoutingTableEntry routingTableEntry : entries) {
            if (nodeNameList.contains(routingTableEntry.getNodeName())) {
                inetSocketAddresses[i] = routingTableEntry.getAddress();
                i++;
            }
        }
        return inetSocketAddresses;
    }

    @Override
    public void stop() {
        if (started) {
            started = false;
            executorService.shutdownNow();
        }
    }
}
