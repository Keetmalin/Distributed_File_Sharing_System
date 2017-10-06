import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Created by Keetmalin on 10/6/2017
 * Project - Distributed_Systems_Project
 */
import java.net.*;
import java.util.StringTokenizer;

public class DistributedNode {

    private int port;
    private String ipAddress;

    private void join_network(int port, String ipAddress) {


        try {
            this.port = port;
            this.ipAddress = ipAddress;

            //create bootstrap request message - Format: 0036 REG 129.82.123.45 5001 1234abcd
            String length = "0036";
            String name = "1234abcd";

            String msg = length + ' ' + "REG" +  ' ' + ipAddress + ' ' + port + ' ' + name;

            //create a datagram packet to send to the Boostrap server
            DatagramPacket datagramPacket = new DatagramPacket(msg.getBytes(), msg.length(),
                    InetAddress.getByName(Constants.BOOTSTRAP_IP) , Constants.BOOTSTRAP_PORT);

            //Create a Datagram Socket
            DatagramSocket datagramSocket = new DatagramSocket(port);

            //send to bootstrap server
            datagramSocket.send(datagramPacket);
            System.out.println("Data Packet Sent to Bootstrap Server: " + msg);

            //start listening to Bootstrap Server Response
            byte[] buffer = new byte[65536];
            DatagramPacket incoming = new DatagramPacket(buffer, buffer.length);
            datagramSocket.receive(incoming);

            String responseMsg = new String(incoming.getData(), 0, incoming.getLength());
            StringTokenizer st = new StringTokenizer(responseMsg, " ");

            String responseLength = st.nextToken();
            String responseCommand = st.nextToken();
            String numberOfNodes = st.nextToken();

            if ("0".equals(responseCommand)){
                System.out.println("request is successful, no nodes in the system");
            }
            else if ("1".equals(responseCommand)){
                System.out.println("request is successful, 1 nodes' contacts will be returned");
            }
            else if ("2".equals(responseCommand) ){
                System.out.println("request is successful, 2 nodes' contacts will be returned");
            }
            else if ("9999 ".equals(responseCommand)){
                System.out.println("failed, there is some error in the command");
            }
            else if ("9998".equals(responseCommand)){
                System.out.println("failed, already registered to you, unregister first");
            }
            else if ("9997".equals(responseCommand)){
                System.out.println("failed, registered to another user, try a different IP and port");
            }
            else if ("9996".equals(responseCommand)){
                System.out.println("failed, can’t register. BS full.");
            }

            System.out.println(responseMsg);

            datagramSocket.close();

        }  catch (IOException e) {
            System.err.println("IOException " + e);
        }

    }

    public static void main(String[] args) throws Exception {

        DistributedNode distributedNode = new DistributedNode();
        distributedNode.join_network(3000, InetAddress.getLocalHost().getHostAddress());
    }
}