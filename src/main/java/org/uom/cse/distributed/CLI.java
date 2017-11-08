/* 
 * <Paste your header here>
 */
package org.uom.cse.distributed;

import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.uom.cse.distributed.peer.Node;
import org.uom.cse.distributed.peer.UDPCommunicationProvider;
import org.uom.cse.distributed.peer.UDPServer;
import org.uom.cse.distributed.peer.api.CommunicationProvider;
import org.uom.cse.distributed.peer.api.NodeServer;
import org.uom.cse.distributed.peer.rest.RestCommunicationProvider;
import org.uom.cse.distributed.peer.rest.RestNodeServer;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.util.UUID;

/**
 * CLI to run the {@link org.uom.cse.distributed.peer.Node}
 *
 * @author Imesha Sudasingha
 * @author Keet Sugathadasa
 */
public class CLI {

    public static void main(String[] args) throws UnknownHostException {
        CmdLineOptions options = new CmdLineOptions();
        CmdLineParser parser = new CmdLineParser(options);

        try {
            parser.parseArgument(args);
        } catch (Exception e) {
            System.err.println("Error occurred when trying to parse command line arguments: " + e.getMessage());
            parser.printUsage(System.err);
            return;
        }

        Constants.BOOTSTRAP_PORT = options.getBsPort();
        Constants.BOOTSTRAP_IP = options.getBsIpAddress();

        Node node;
        try {
            CommunicationProvider cp;
            NodeServer ns;
            if (options.isRest()) {
                cp = new RestCommunicationProvider();
                ns = new RestNodeServer();
            } else {
                cp = new UDPCommunicationProvider();
                ns = new UDPServer(options.getPort());
            }

            node = new Node(options.getPort(), options.getIpAddress(), options.getUsername(), cp, ns);
            node.start();
            System.out.println("Node started ...");
            Runtime.getRuntime().addShutdownHook(new Thread(node::stop));
        } catch (Exception e) {
            System.err.println("Error occurred: " + e.getMessage());
            return;
        }

        System.out.println("Enter <help> for help menu\n");
        Scanner scanner = new Scanner(System.in);
        boolean started = true;
        while (started) {
            String command = scanner.nextLine();
            String[] parts = command.split(" ", 2);

            try {
                switch (parts[0]) {
                    case "stop":
                        node.stop();
                        started = false;
                        break;
                    case "node":
                        System.out.println(node.getNodeId());
                        break;
                    case "state":
                        System.out.println(node.getState());
                        break;
                    case "search":
                        node.getUdpQuery().searchFullFile(parts[1]);
                        break;
                    case "searchK":
                        node.getUdpQuery().searchKeyword(parts[1]);
                        break;
                    case "routingTable":
                        node.getRoutingTable().getEntries()
                                .forEach(entry -> System.out.println(entry.getNodeId() + " -> " + entry.getAddress().toString()));
                        break;
                    case "myFiles":
                        System.out.println(node.getMyFiles());
                        break;
                    case "entryTable":
                        StringBuilder sb = new StringBuilder();
                        node.getEntryTable().getEntries().forEach(((character, stringListMap) -> {
                            sb.append("\n");
                            sb.append(character);
                            sb.append("\t-> ");
                            if (stringListMap.size() > 0) {
                                sb.append("\n");
                            }

                            stringListMap.forEach((keyword, entries) -> {
                                if (entries.size() > 0) {
                                    sb.append("\t");
                                    sb.append(keyword);
                                    sb.append("\t-> ");
                                    sb.append(entries);
                                    sb.append("\n");
                                }
                            });
                        }));
                        System.out.println(sb.toString());
                        break;
                    case "help":
                        System.out.println("stop | node | state | search | routingTable | myFiles | entryTable");
                        break;
                    default:
                        System.out.println("Command not identified");
                }
            } catch (Exception e) {
                System.err.println("Error occurred: " + e.getMessage());
            }
        }

        System.out.println("Stopping CLI ...");
    }

    private static class CmdLineOptions {

        @Option(name = "-port", usage = "Port of the distributed node. (default: 32050)")
        private int port = 32050;

        @Option(name = "-ip", usage = "IP address of the node. (default: localhost)")
        private String ipAddress = String.valueOf(InetAddress.getLocalHost().getHostAddress());

        @Option(name = "-bs-ip", usage = "IP address of the Bootstrap Server. (default: localhost)")
        private String bsIpAddress = Constants.BOOTSTRAP_IP;

        @Option(name = "-bs-port", usage = "IP address of the Bootstrap Server. (default: localhost)")
        private int bsPort = Constants.BOOTSTRAP_PORT;

        @Option(name = "-username", usage = "Username of the node (default: A random UUID)")
        private String username = UUID.randomUUID().toString();

        @Option(name = "-rest", usage = "Use REST services over UDP sockets if this option is given")
        private boolean isRest = false;

        private CmdLineOptions() throws UnknownHostException { }

        public int getPort() {
            return port;
        }

        public String getIpAddress() {
            return ipAddress;
        }

        public String getUsername() {
            return username;
        }

        public String getBsIpAddress() {
            return bsIpAddress;
        }

        public int getBsPort() {
            return bsPort;
        }

        public boolean isRest() {
            return isRest;
        }
    }
}
