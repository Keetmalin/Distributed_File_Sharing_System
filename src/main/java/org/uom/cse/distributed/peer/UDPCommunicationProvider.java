package org.uom.cse.distributed.peer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uom.cse.distributed.peer.api.CommunicationProvider;
import org.uom.cse.distributed.peer.utils.RequestUtils;

import java.io.ByteArrayInputStream;
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

import static org.uom.cse.distributed.Constants.*;

/**
 * Provides UDP Socket Based communication with Peers
 *
 * @author Keet Sugathadasa
 */
public class UDPCommunicationProvider extends CommunicationProvider {

    private static final Logger logger = LoggerFactory.getLogger(Node.class);
    private final int numOfRetries = RETRIES_COUNT;
    private ExecutorService executorService;

    public void start() {
        executorService = Executors.newCachedThreadPool();
        logger.info("Communication provider started");
    }

    @SuppressWarnings("unchecked")
    @Override
    public Set<RoutingTableEntry> connect(InetSocketAddress peer) {
        //TODO check build request method
        String request = RequestUtils.buildRequest(GET_ROUTING_TABLE);
        logger.debug("Sending request ({}) to get routing table from {}", request, peer);
        String response = retryOrTimeout(request, peer);
        logger.debug("Received response : {}", response);
        if (response != null) {
            byte[] received = Base64.getDecoder().decode(response);
            ByteArrayInputStream bais = new ByteArrayInputStream(received);
            try (ObjectInputStream in = new ObjectInputStream(bais)) {
                Object obj = in.readObject();
                logger.debug("Received routing table entries. {} - {}", obj.getClass(), obj);
                return (HashSet<RoutingTableEntry>) obj;
            } catch (Exception e) {
                logger.error("Error occurred when obtaining routing table", e);
            }
        }

        // If failed we return an empty set to not to break operations.
        return new HashSet<>();
    }

    @Override
    public boolean disconnect(InetSocketAddress peer) {
        return false;
    }

    @Override
    public Map<String, Map<String, List<Integer>>> notifyNewNode(InetSocketAddress peer, InetSocketAddress me, int nodeId) {
        String request = String.format(NEWNODE_MSG_FORMAT, me.getHostName(), me.getPort(), nodeId);
        //String request = RequestUtils.buildRequest(msg);
        logger.debug("Notifying new node to {} as message: {}", peer, request);
        String response = retryOrTimeout(request, peer);
        logger.debug("Received response : {}", response);
        return new HashMap<>();
    }

    @Override
    public void offerFile(InetSocketAddress peer, String keyword, String node , String file ){

        String request = NEW_ENTRY + " " + keyword + " " + node + " " + file;
        retryOrTimeout( request , peer);
    }

    /**
     * This method retries a given requests or times out of that request fails. Tries for maximum of {@link
     * UDPCommunicationProvider#numOfRetries}
     *
     * @param request request to be sent
     * @param peer    to whom the request is sent
     * @return null | response
     */
    private String retryOrTimeout(String request, InetSocketAddress peer) {
        int retriesLeft = numOfRetries;
        while (retriesLeft > 0) {
            Future<String> task = executorService.submit(() -> {
                try (DatagramSocket datagramSocket = new DatagramSocket()) {
                    return RequestUtils.sendRequest(datagramSocket, request, peer.getAddress(), peer.getPort());
                }
            });

            try {
                //TODO fix this - it retries for 3 times always
                return task.get(RETRY_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                logger.error("Error occurred when completing request({}) to peer- {}. Error: {}", request, peer, e.getMessage());
                task.cancel(true);
                retriesLeft--;
            }
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
