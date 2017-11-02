package org.uom.cse.distributed.peer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uom.cse.distributed.peer.api.CommunicationProvider;
import org.uom.cse.distributed.peer.api.EntryTableEntry;
import org.uom.cse.distributed.peer.api.RoutingTableEntry;
import org.uom.cse.distributed.peer.utils.RequestUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.uom.cse.distributed.Constants.GET_ROUTING_TABLE;
import static org.uom.cse.distributed.Constants.NEWENTRY_MSG_FORMAT;
import static org.uom.cse.distributed.Constants.NEWNODE_MSG_FORMAT;
import static org.uom.cse.distributed.Constants.PING_MSG_FORMAT;
import static org.uom.cse.distributed.Constants.QUERY_MSG_FORMAT;
import static org.uom.cse.distributed.Constants.RESPONSE_FAILURE;
import static org.uom.cse.distributed.Constants.RETRIES_COUNT;
import static org.uom.cse.distributed.Constants.RETRY_TIMEOUT_MS;

/**
 * Provides UDP Socket Based communication with Peers
 *
 * @author Imesha Sudasingha
 * @author Keet Sugathadasa
 */
public class UDPCommunicationProvider extends CommunicationProvider {

    private static final Logger logger = LoggerFactory.getLogger(Node.class);
    private final int numOfRetries = RETRIES_COUNT;
    private ExecutorService executorService;
    private Node node;

    @Override
    public void start(Node node) {
        this.node = node;
        executorService = Executors.newCachedThreadPool();
        logger.info("Communication provider started");
    }

    @SuppressWarnings("unchecked")
    @Override
    public Set<RoutingTableEntry> connect(InetSocketAddress peer) {
        String request = RequestUtils.buildRequest(GET_ROUTING_TABLE);
        logger.debug("Sending request ({}) to get routing table from {}", request, peer);
        String response = retryOrTimeout(request, peer);
        logger.debug("Received response : {}", response);
        if (response != null) {
            Object obj = RequestUtils.base64StringToObject(response.split(" ")[3]);
            logger.debug("Received routing table entries -> {}", obj);
            if (obj != null) {
                return (HashSet<RoutingTableEntry>) obj;
            }
        }

        // If failed we return an empty set to not to break operations.
        return new HashSet<>();
    }

    @Override
    public boolean disconnect(InetSocketAddress peer) {
        return false;
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public Map<Character, Map<String, List<EntryTableEntry>>> notifyNewNode(InetSocketAddress peer,
            InetSocketAddress me, int nodeId) {
        String msg = String.format(NEWNODE_MSG_FORMAT, me.getHostName(), me.getPort(), nodeId);
        String request = RequestUtils.buildRequest(msg);
        logger.debug("Notifying new node to {} as message: {}", peer, request);
        String response = retryOrTimeout(request, peer);
        logger.debug("Received response : {}", response);
        if (response != null) {
            Object obj = RequestUtils.base64StringToObject(response.split(" ")[3]);
            logger.debug("Received characters to be taken over -> {}", obj);
            if (obj != null) {
                return (Map<Character, Map<String, List<EntryTableEntry>>>) obj;
            }
        }

        return new HashMap<>();
    }

    @Override
    public boolean offerFile(InetSocketAddress peer, String keyword, int nodeId, String file) {
        String msg = String.format(NEWENTRY_MSG_FORMAT, keyword, nodeId, file);
        String request = RequestUtils.buildRequest(msg);
        logger.debug("Offering file with request: {}", request);
        String response = retryOrTimeout(request, peer);
        logger.debug("Received response: {}", response);
        return response != null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Set<InetSocketAddress> searchFullFile(InetSocketAddress targetNode, String fileName, String keyword) {
        String request = String.format(QUERY_MSG_FORMAT, keyword, fileName);
        logger.debug("Searching filename: {} , with keyword: {} in the network", fileName, keyword);
        String response = retryOrTimeout(request, targetNode);
        logger.debug("Received response: {}", response);

        if (response != null) {
            byte[] received = Base64.getDecoder().decode(response);
            ByteArrayInputStream bais = new ByteArrayInputStream(received);
            try (ObjectInputStream in = new ObjectInputStream(bais)) {
                Object obj = in.readObject();
                logger.debug("Received the Set of addresses that contains the file {}. {} - {}", fileName, obj.getClass(), obj);
                return (HashSet<InetSocketAddress>) obj;
            } catch (Exception e) {
                logger.error("Error occurred when obtaining routing table", e);
            }
        }

        // If failed we return an empty set to not to break operations.
        return new HashSet<>();
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    public Map<Character, Map<String, List<EntryTableEntry>>> ping(InetSocketAddress peer,
            Map<Character, Map<String, List<EntryTableEntry>>> toBeHandedOver) {
        String base64 = null;
        try {
            base64 = RequestUtils.buildObjectRequest(toBeHandedOver);
        } catch (IOException e) {
            logger.error("Error occurred when encoding entries to be handed over to -> {}", peer, e);
            throw new IllegalArgumentException("Unable to encode entries", e);
        }

        String msg = String.format(PING_MSG_FORMAT, this.node.getNodeId(), base64);
        String request = RequestUtils.buildRequest(msg);
        logger.debug("Pinging -> {}", peer);
        String response = retryOrTimeout(1, request, peer);
        logger.debug("Received response : {}", response);
        if (response != null) {
            Object obj = RequestUtils.base64StringToObject(response);
            logger.debug("Received entry table of ({}) -> {}", peer, obj);
            if (obj != null) {
                return (Map<Character, Map<String, List<EntryTableEntry>>>) obj;
            }
        }

        return null;
    }

    /**
     * @see #retryOrTimeout(int, String, InetSocketAddress)
     */
    private String retryOrTimeout(String request, InetSocketAddress peer) {
        return retryOrTimeout(numOfRetries, request, peer);
    }

    /**
     * This method retries a given requests or times out of that request fails. Tries for maximum of {@link
     * UDPCommunicationProvider#numOfRetries}
     *
     * @param retries Number of times should be retried
     * @param request request to be sent
     * @param peer    to whom the request is sent
     * @return null | response
     */
    private String retryOrTimeout(int retries, String request, InetSocketAddress peer) {
        int retriesLeft = retries;
        while (retriesLeft > 0) {
            Future<String> task = executorService.submit(() -> {
                try (DatagramSocket datagramSocket = new DatagramSocket()) {
                    return RequestUtils.sendRequest(datagramSocket, request, peer.getAddress(), peer.getPort());
                }
            });

            try {
                String response = task.get(RETRY_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                if (!response.contains(RESPONSE_FAILURE)) {
                    return response;
                }
            } catch (Exception e) {
                logger.error("Error occurred when completing request({}) to peer -> {}. Error: {}", request, peer, e);
                task.cancel(true);
                retriesLeft--;
            }
        }

        logger.error("REQUEST FAILED !!! ({} -> {})", request, peer);
        if (retries == numOfRetries) {
            this.node.removeNode(peer);
        }
        return null;
    }

    public void stop() {
        if (executorService != null) {
            executorService.shutdownNow();
            executorService = null;
        }
        logger.info("Communication provider stopped");
    }
}
