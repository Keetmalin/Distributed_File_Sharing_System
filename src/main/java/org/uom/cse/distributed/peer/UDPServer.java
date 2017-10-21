package org.uom.cse.distributed.peer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This class implements the server side listening and handling of requests
 * Via UDP - for each node in the Distributed Network
 *
 * @author Keet Sugathadasa
 */
public class UDPServer{

    private ExecutorService executorService;
    private static final Logger logger = LoggerFactory.getLogger(Node.class);
    private final Node node;
    private boolean started = false;

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

                if ("RoutingTable".equals(incomingMsg)){
                    provideRoutingTable(incoming , datagramSocket);
                } else if ("Peer".equals(incomingMsg)){
                    addPeer("ipaddress", 2222);
                }


            }

        }catch (IOException e){
            logger.error("Error occurred when listening on Port", e);
        }
    }

    private void sendResponse(String response , DatagramPacket incoming , DatagramSocket datagramSocket) throws IOException{
        DatagramPacket dpReply = new DatagramPacket(response.getBytes(), response.getBytes().length,
                incoming.getAddress(), incoming.getPort());
        datagramSocket.send(dpReply);
    }

    private void provideRoutingTable(DatagramPacket incoming , DatagramSocket datagramSocket) throws IOException {

        final ByteArrayOutputStream baos = new ByteArrayOutputStream(6400);
        final ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(this.node.getRoutingTable());
        final byte[] data = baos.toByteArray();

        DatagramPacket dpReply = new DatagramPacket(data, data.length,
                incoming.getAddress(), incoming.getPort());
        datagramSocket.send(dpReply);

    }

    private void addPeer(String ipAddress, int port) throws UnknownHostException {
        this.node.addPeer(new InetSocketAddress(ipAddress , port));
    }

    public void stop(){

        if (started) {
            started = false;
            executorService.shutdownNow();
        }
    }
}
