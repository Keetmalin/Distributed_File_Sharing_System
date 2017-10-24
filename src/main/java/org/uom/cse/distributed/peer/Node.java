package org.uom.cse.distributed.peer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uom.cse.distributed.peer.api.BootstrapProvider;
import org.uom.cse.distributed.peer.api.CommunicationProvider;
import org.uom.cse.distributed.peer.api.EntryTable;
import org.uom.cse.distributed.peer.api.EntryTableEntry;
import org.uom.cse.distributed.peer.api.Server;
import org.uom.cse.distributed.peer.api.State;
import org.uom.cse.distributed.peer.api.StateManager;
import org.uom.cse.distributed.peer.utils.HashUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.uom.cse.distributed.Constants.ADDRESSES_PER_CHARACTER;
import static org.uom.cse.distributed.Constants.ADDRESS_SPACE_SIZE;
import static org.uom.cse.distributed.Constants.FILE_NAME_ARRAY;
import static org.uom.cse.distributed.Constants.MAX_FILE_COUNT;
import static org.uom.cse.distributed.Constants.MIN_FILE_COUNT;
import static org.uom.cse.distributed.peer.api.State.CONFIGURED;
import static org.uom.cse.distributed.peer.api.State.CONNECTED;
import static org.uom.cse.distributed.peer.api.State.IDLE;
import static org.uom.cse.distributed.peer.api.State.REGISTERED;

/**
 * The class to represent a Node in the distributed network.
 *
 * @author Keet Sugathadasa
 * @author Imesha Sudasingha
 */
public class Node {

    private static final Logger logger = LoggerFactory.getLogger(Node.class);

    private final StateManager stateManager = new StateManager(IDLE);
    private final RoutingTable routingTable = new RoutingTable();
    private final EntryTable entryTable = new EntryTable();
    private final List<String> myFiles = new ArrayList<>();

    private final CommunicationProvider communicationProvider;
    private final UDPQuery udpQuery = new UDPQuery();
    private final Server server;
    private final String username;
    private final String ipAddress;
    private final int port;
    private int nodeId;
    private char myChar;

    private BootstrapProvider bootstrapProvider = new UDPBootstrapProvider();

    public Node(int port) {
        this(port, new UDPCommunicationProvider(), new UDPServer(port));
    }

    public Node(int port, CommunicationProvider communicationProvider, Server server) {
        this(port, "localhost", communicationProvider, server);
    }

    public Node(int port, String ipAddress, CommunicationProvider communicationProvider, Server server) {
        this(port, ipAddress, UUID.randomUUID().toString(), communicationProvider, server);
    }

    public Node(int port, String ipAddress, String username, CommunicationProvider communicationProvider, Server server) {
        this.port = port;
        this.ipAddress = ipAddress;
        this.username = username;
        this.communicationProvider = communicationProvider;
        this.server = server;
    }

    public void start() {
        stateManager.checkState(State.IDLE);

        server.start(this);
        communicationProvider.start();
        udpQuery.initialize(this);

        // 1. Register and fetch 2 random peers from Bootstrap Server
        logger.debug("Registering node");
        List<InetSocketAddress> peers;
        try {
            peers = bootstrapProvider.register(ipAddress, port, username);
        } catch (IOException e) {
            logger.error("Error occurred when registering node", e);
            throw new IllegalStateException("Unable to register this node", e);
        }

        if (peers == null) {
            logger.error("Peers are null");
            throw new IllegalStateException("Unable to register successfully");
        }

        stateManager.setState(REGISTERED);
        logger.info("Node registered successfully", ipAddress, port);

        // 2. Connect to the peers send by BS and fetch their routing tables
        logger.info("Collecting routing table from peers: {}", peers);
        peers.forEach(peer -> {
            //TODO check routing table update - this doesn't retrieve the tables
            Set<RoutingTableEntry> entries = communicationProvider.connect(peer);
            logger.debug("Received routing table: {} from {}", entries, peer);
            entries.forEach(routingTable::addEntry);
        });
        stateManager.setState(CONNECTED);

        logger.info("Successfully connected to the network and created routing table");

        // 3. Select a Node Name
        this.nodeId = selectNodeName();
        logger.info("Selected node ID -> {}", this.nodeId);

        // 4. Select my characters
        myChar = HashUtils.nodeIdToChar(this.nodeId);
        logger.info("My char is -> {}", myChar);
        entryTable.addCharacter(myChar);

        Optional<RoutingTableEntry> myPredecessor = routingTable.findPredecessorOf(this.nodeId);
        logger.debug("My predecessor is -> {}", myPredecessor);
        Set<Character> characters = HashUtils.findCharactersOf(this.nodeId, myPredecessor.map(routingTableEntry ->
                Integer.parseInt(routingTableEntry.getNodeName())).orElse(this.nodeId));
        characters.forEach(entryTable::addCharacter);


        // 5. Broadcast that I have joined the network to all entries in the routing table
        this.routingTable.getEntries().forEach(entry -> {
            //TODO check the Notify other nodes broadcast
            Map<Character, Map<String, List<EntryTableEntry>>> toBeUndertaken = communicationProvider.notifyNewNode(
                    entry.getAddress(), new InetSocketAddress(ipAddress, port), this.nodeId);

            toBeUndertaken.forEach((letter, keywordMap) -> {
                logger.info("Undertaking letter [{}] and keywords -> {}", letter, keywordMap);

                // First put the letter [A-Z0-9]
                entryTable.addCharacter(letter);

                // Then put the keywords under each letter
                keywordMap.forEach((keyword, entryTableEntries) -> {
                    entryTableEntries.forEach(entryTableEntry -> {
                        logger.debug("Adding entry-{} for keyword: {} to entry table", entryTableEntry, keyword);
                        entryTable.addEntry(keyword, entryTableEntry);
                    });
                });
            });
        });

        // 6. Add my node to my routing table
        routingTable.addEntry(new RoutingTableEntry(new InetSocketAddress(ipAddress, port), String.valueOf(this.nodeId)));
        logger.debug("My routing table is -> {}", routingTable.getEntries());

        // 7. Send my files to corresponding nodes.
        myFiles.addAll(generateMyFiles());
        myFiles.forEach(file -> {
            String keywords[] = file.split(" ");
            Stream.of(keywords).forEach(keyword -> {
                int nodeId = HashUtils.keywordToNodeId(keyword);
                logger.debug("NodeId -> {} to index keyword -> {}", nodeId, keyword);
                Optional<RoutingTableEntry> entry = routingTable.findNodeOrSuccessor(nodeId);
                logger.debug("Searching for node or successor in routing table -> {}", entry);

                // Usually an entry should be present.
                if (entry.isPresent() && Integer.valueOf(entry.get().getNodeName()) != this.nodeId) {
                    logger.info("Offering keyword ({}-{}) to Node - {}", keyword, file, entry.get());
                    communicationProvider.offerFile(entry.get().getAddress(), keyword, this.nodeId, file);
                } else {
                    // I should take over this file name
                    logger.info("I'm indexing ({}-{})", keyword, file);
                    entryTable.addEntry(keyword, new EntryTableEntry(String.valueOf(this.nodeId), file));
                }
            });
        });

        stateManager.setState(CONFIGURED);
    }

    /**
     * Selects a Node Name for the newly connected node (this one). When selecting, we chose a random node name within
     * <strong>1 - 180</strong> which maps from <strong>[A-Z0-9] -> [1-180]</strong>.
     *
     * @return The selected node name
     */
    private int selectNodeName() {
        Set<Integer> usedNodes = this.routingTable.getEntries().stream()
                .map(entry -> Integer.parseInt(entry.getNodeName()) / ADDRESSES_PER_CHARACTER)
                .collect(Collectors.toSet());

        Random random = new Random();
        // We can allow up to 36 Nodes in our network this way.
        while (true) {
            int candidate = 1 + random.nextInt(ADDRESS_SPACE_SIZE);
            if (!usedNodes.contains(candidate)) {
                return candidate;
            }
        }
    }

    /**
     * Generates and Returns the list of files available in my node. 3 to 5 files in each node
     *
     * @return List of files available in my node.
     */
    private List<String> generateMyFiles() {
        if (myFiles.size() == 0) {
            //randomly decide the file count to be 3 to 5 files
            Random random = new Random();
            int fileCount = random.nextInt((MAX_FILE_COUNT - MIN_FILE_COUNT) + 1) + MIN_FILE_COUNT;

            List<String> tempList = Arrays.asList(FILE_NAME_ARRAY);
            Collections.shuffle(tempList);
            return tempList.subList(0, 1);
        }
        return myFiles;
    }


    public void addNewNode(String ipAddress, int newNodePort, int newNodeId) {
        InetSocketAddress inetSocketAddress = new InetSocketAddress(ipAddress, newNodePort);
        RoutingTableEntry routingTableEntry = new RoutingTableEntry(inetSocketAddress, String.valueOf(newNodeId));
        routingTable.addEntry(routingTableEntry);
    }


    public Map<Character, Map<String, List<EntryTableEntry>>> getEntriesToHandoverTo(int nodeId) {
        // 1. Find the predecessor of the node given
        Optional<RoutingTableEntry> entryOptional = routingTable.findPredecessorOf(nodeId);
        if (!entryOptional.isPresent()) {
            logger.warn("No predecessor found for node -> {}", nodeId);
            return null;
        }

        // 2. Now find the characters which should be handled by the new node. i.e: From its predecessor to new node
        RoutingTableEntry predecessor = entryOptional.get();
        logger.debug("Found predecessor {} for node -> {}", predecessor, nodeId);
        Set<Character> characters = HashUtils.findCharactersOf(nodeId, Integer.parseInt(predecessor.getNodeName()));

        // 3. Collect the entries for those characters
        Map<Character, Map<String, List<EntryTableEntry>>> toBeHandedOver = new HashMap<>();
        characters.forEach(character -> {
            Map<String, List<EntryTableEntry>> keywords = entryTable.getKeywordsFor(character);
            if (keywords != null) {
                toBeHandedOver.put(character, keywords);
            }
        });

        return toBeHandedOver;
    }


    public void removeEntries(Set<Character> characters) {
        characters.forEach(entryTable::removeCharacter);
    }


    public void stop() {
        // TODO: graceful departure
        logger.debug("Stopping node");
        if (stateManager.getState().compareTo(REGISTERED) >= 0) {

            if (stateManager.getState().compareTo(CONNECTED) >= 0) {
                // TODO: 10/21/17 Notify all the indexed nodes that I'm leaving
                // TODO: 10/20/17 Should we disconnect from the peers or all entries in the routing table?
                this.routingTable.getEntries().forEach(entry -> {
                    if (communicationProvider.disconnect(entry.getAddress())) {
                        logger.debug("Successfully disconnected from {}", entry);
                    } else {
                        logger.warn("Unable to disconnect from {}", entry);
                    }
                });

                this.routingTable.clear();
                this.myFiles.clear();
                this.entryTable.clear();
                stateManager.setState(REGISTERED);
            }

            try {
                bootstrapProvider.unregister(ipAddress, port, username);
            } catch (IOException e) {
                logger.error("Error occurred when unregistering", e);
            }
        }

        communicationProvider.stop();
        server.stop();
        stateManager.setState(IDLE);
        logger.info("Distributed node stopped");
    }

    public int getPort() {
        return port;
    }

    public String getUsername() {
        return username;
    }

    public List<String> getMyFiles() {
        return myFiles;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public RoutingTable getRoutingTable() {
        return routingTable;
    }

    public EntryTable getEntryTable() {
        return entryTable;
    }

    public State getState() {
        return stateManager.getState();
    }

    public CommunicationProvider getCommunicationProvider() {
        return communicationProvider;
    }

    @Override
    public void finalize() throws Throwable {
        stop();
        super.finalize();
    }
}