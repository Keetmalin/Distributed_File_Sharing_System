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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.uom.cse.distributed.Constants.GET_ROUTING_TABLE;
import static org.uom.cse.distributed.Constants.JOIN;
import static org.uom.cse.distributed.Constants.NEW_ENTRY;
import static org.uom.cse.distributed.Constants.RESPONSE_OK;
import static org.uom.cse.distributed.Constants.RETRIES_COUNT;

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
                String incomingMsg = new String(data, 0, incoming.getLength());
                logger.debug("Received from {}:{} - {}", incoming.getAddress(), incoming.getPort(), incomingMsg);

                try {
                    handleRequest(incomingMsg, incoming);
                } catch (Exception e) {
                    logger.error("Error occurred when handling request", e);
                }
            }
        } catch (IOException e) {
            logger.error("Error occurred when listening on port {}", port, e);
            throw new IllegalStateException("Error occurred when listening", e);
        }
    }

    private void handleRequest(String request, DatagramPacket incoming) {
        String[] incomingResult = request.split(" ", 3);

        logger.debug("Request length: {}", incomingResult[0]);
        String command = incomingResult[1];
        logger.debug("Command: {}", command);

        if (GET_ROUTING_TABLE.equals(command)) {
            provideRoutingTable(incoming);
        } else if (NEW_ENTRY.equals(command)) {
            String[] tempList = incomingResult[2].split(" ", 3);
            logger.debug("Adding entry to entry table: {}", tempList);
            this.node.getEntryTable().addEntry(tempList[0], new EntryTableEntry(tempList[1], tempList[2]));
        } else if (JOIN.equals(command)) {
            //String ipAddress = st.nextToken();
            //int port = Integer.parseInt(st.nextToken());
            //                    handleBroadcastRequest(nodeName, incoming, ipAddress, port);
        }

    }

    @Override
    public void provideRoutingTable(DatagramPacket incoming) {
        int retriesLeft = numOfRetries;
        while (retriesLeft > 0) {
            try (DatagramSocket datagramSocket = new DatagramSocket()) {
                RequestUtils.sendObjectRequest(datagramSocket, this.node.getRoutingTable().getEntries(),
                        incoming.getAddress(), incoming.getPort());
                logger.debug("Routing table entries provided to the recipient: {}", incoming.getAddress(), incoming.getPort());
                break;
            } catch (IOException e) {
                logger.error("Error occurred when sending the response", e);
                retriesLeft--;
            }
        }
    }

    @Override
    public void handleBroadcastRequest(String nodeName, DatagramPacket datagramPacket, String ipAddress, int port) {

        InetSocketAddress inetSocketAddress = new InetSocketAddress(ipAddress, port);
        RoutingTableEntry routingTableEntry = new RoutingTableEntry(inetSocketAddress, nodeName);

        this.node.getRoutingTable().addEntry(routingTableEntry);

        int retriesLeft = numOfRetries;
        while (retriesLeft > 0) {
            try (DatagramSocket datagramSocket = new DatagramSocket()) {
                //TODO Return files belonging to that node
                RequestUtils.sendResponse(datagramSocket, RESPONSE_OK, datagramPacket.getAddress(),
                        datagramPacket.getPort());
                logger.debug("Response Ok sent to the recipient");

            } catch (IOException e) {
                logger.error("Error occurred when sending the response", e);
                retriesLeft--;
            }

        }


    }

    public void stop() {
        if (started) {
            started = false;
            executorService.shutdownNow();
        }
    }
}
