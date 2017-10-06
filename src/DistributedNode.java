import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Created by Keetmalin on 10/6/2017
 * Project - Distributed_Systems_Project
 */
import java.net.*;

public class DistributedNode {

    public void join_network() {


        try {

            //create bootstrap request message - Format: 0036 REG 129.82.123.45 5001 1234abcd
            String length = "0036";
            String ipAddress = InetAddress.getLocalHost().getHostAddress();
            int port = 3000;
            String name = "1234abcd";

            String msg = length + ' ' + "REG" +  ' ' + ipAddress + ' ' + port + ' ' + name;

            //create a datagram packet to send to the Boostrap server
            DatagramPacket datagramPacket = new DatagramPacket(msg.getBytes(), msg.length(),
                    InetAddress.getByName(Constants.BOOTSTRAP_IP) , Constants.BOOTSTRAP_PORT);

            //Create a Datagram Socket
            DatagramSocket datagramSocket = new DatagramSocket();

            //send to bootstrap server
            datagramSocket.send(datagramPacket);
            System.out.println("Data Packet Sent to Bootstrap Server: " + msg);

            datagramSocket.close();

            //Create a Datagram Socket
            DatagramSocket datagramSocketListener = new DatagramSocket(port);

            //start listening to Bootstrap Server Response
            byte[] buffer = new byte[65536];
            DatagramPacket incoming = new DatagramPacket(buffer, buffer.length);
            datagramSocketListener.receive(incoming);

            String responseMsg = new String(datagramPacket.getData(), 0, datagramPacket.getLength());
            System.out.println(responseMsg);

            datagramSocketListener.close();

        } catch (SocketException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void main(String[] args) throws Exception {

        DistributedNode distributedNode = new DistributedNode();
        distributedNode.join_network();
    }
}