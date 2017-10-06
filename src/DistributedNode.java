/**
 * Created by Keetmalin on 10/6/2017
 * Project - Distributed_Systems_Project
 */

import java.net.*;


public class DistributedNode {

    public static void main(String[] args) throws Exception {
        DatagramSocket ds = new DatagramSocket();
        String str = "My name is keet";
        InetAddress ip = InetAddress.getLocalHost();

        DatagramPacket dp = new DatagramPacket(str.getBytes(), str.length(), ip, 55555);
        ds.send(dp);
        ds.close();
    }
}
