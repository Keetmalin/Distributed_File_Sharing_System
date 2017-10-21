package org.uom.cse.distributed.peer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uom.cse.distributed.peer.api.Server;
import org.uom.cse.distributed.peer.utils.RequestUtils;

import java.io.IOException;
import java.net.*;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.uom.cse.distributed.Constants.*;

/**
 * This class implements the server side listening and handling of requests
 * Via UDP - for each node in the Distributed Network
 *
 * @author Keet Sugathadasa
 */
public class UDPServer implements Server{

    private ExecutorService executorService;
    private static final Logger logger = LoggerFactory.getLogger(Node.class);
    private final Node node;
    private boolean started = false;
    private final int numOfRetries = RETRIES_COUNT;

    public UDPServer(Node node){
        this.node = node;
        this.started = true;
    }

    public void start() {
        if (started) {
            logger.warn("Listener already running");
            return;
        }

        started = true;
        executorService = Executors.newSingleThreadExecutor();
        executorService.submit(() -> {
            try {
                listen();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
    }


    @Override
    public void listen() {

        DatagramSocket datagramSocket = null;
        String incomingMsg;


        try{
            datagramSocket = new DatagramSocket(this.node.getPort());
            logger.debug("Node is Listening to incoming requests");

            while (started) {
                byte[] buffer = new byte[65536];
                DatagramPacket incoming = new DatagramPacket(buffer, buffer.length);
                datagramSocket.receive(incoming);

                byte[] data = incoming.getData();
                incomingMsg = new String(data, 0, incoming.getLength());

                //log the details of the incoming message
                logger.debug(incoming.getAddress().getHostAddress() + " : " + incoming.getPort() + " - "
                        + incomingMsg);

                StringTokenizer st = new StringTokenizer(incomingMsg, " ");
                //every incoming message comes with REQUEST + NODENAME

                String request = st.nextToken();
                String nodeName = st.nextToken();

                if (GETROUTINGTABLE.equals(request)){
                    provideRoutingTable(incoming);
                } else if (BROADCAST.equals(request)) {
                    String ipAddress = st.nextToken();
                    int port = Integer.parseInt(st.nextToken());
                    handleBroadcastRequest(nodeName, incoming, ipAddress, port);
                }


            }

        }catch (IOException e){
            logger.error("Error occurred when listening on Port", e);
        }
    }

//    private void sendResponse(String response , DatagramPacket incoming , DatagramSocket datagramSocket) throws IOException{
//        DatagramPacket dpReply = new DatagramPacket(response.getBytes(), response.getBytes().length,
//                incoming.getAddress(), incoming.getPort());
//        datagramSocket.send(dpReply);
//    }

    @Override
    public void provideRoutingTable(DatagramPacket incoming) {

        int retriesLeft = numOfRetries;
        while (retriesLeft > 0) {

            try (DatagramSocket datagramSocket = createDatagramSocket()) {
                RequestUtils.sendObjectRequest(datagramSocket, this.node.getRoutingTable().getEntries(),
                        incoming.getAddress(), incoming.getPort());
                logger.debug("Routing table entries provided to the recipient");

            } catch (IOException e) {
                logger.error("Error occurred when sending the response", e);
                retriesLeft--;
            }

        }
    }

    @Override
    public void handleBroadcastRequest(String nodeName , DatagramPacket datagramPacket, String ipAddress, int port){

        InetSocketAddress inetSocketAddress = new InetSocketAddress(ipAddress, port);
        RoutingTableEntry routingTableEntry = new RoutingTableEntry(inetSocketAddress, nodeName);

        this.node.getRoutingTable().addEntry(routingTableEntry);

        int retriesLeft = numOfRetries;
        while (retriesLeft > 0) {

            try (DatagramSocket datagramSocket = createDatagramSocket()) {
                //TODO Return files belonging to that node
                RequestUtils.sendResponse(datagramSocket, RESPONSE_OK,
                        datagramPacket.getAddress(), datagramPacket.getPort());
                logger.debug("Response Ok sent to the recipient");

            } catch (IOException e) {
                logger.error("Error occurred when sending the response", e);
                retriesLeft--;
            }

        }


    }

    private DatagramSocket createDatagramSocket() throws SocketException {
        int port = this.node.getPort() + new Random().nextInt(55536);
        logger.debug("Creating Datagram Socket at port : {}", port);
        return new DatagramSocket(port);
    }

//    @Override
//    public void broadcast(String nodeName , DatagramPacket datagramPacket) {
//
//        String request = buildNewNodeEntry();
//
//        Set<RoutingTable.RoutingTableEntry> routingEntries = node.getRoutingTable().getEntries();
//        for (RoutingTable.RoutingTableEntry entry: routingEntries) {
//
//            int retriesLeft = numOfRetries;
//            while (retriesLeft > 0) {
//                try (DatagramSocket datagramSocket = createDatagramSocket()) {
//                    String response = RequestUtils.sendRequest(datagramSocket, request, entry.getAddress().getAddress(),
//                            entry.getPort());
//                    logger.debug("Response received : {}", response);
//                } catch (IOException e) {
//                    logger.error("Error occurred when sending the request", e);
//                    retriesLeft--;
//                }
//            }
//
//        }
//
//        logger.debug("broadcast to all entries in the routing table complete");
//
//
//
//    }
//
//    private String buildNewNodeEntry(){
//        return NEW_NODE_ENTRY + " " + node.getUsername() + " " + node.getIpAddress() + " " + String.valueOf(node.getPort());
//    }

//    private void addPeer(String ipAddress, int port) throws UnknownHostException {
//        this.node.addPeer(new InetSocketAddress(ipAddress , port));
//    }

    public void stop(){

        if (started) {
            started = false;
            executorService.shutdownNow();
        }
    }
}
