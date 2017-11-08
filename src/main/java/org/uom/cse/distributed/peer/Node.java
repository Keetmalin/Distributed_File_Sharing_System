package org.uom.cse.distributed.peer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uom.cse.distributed.peer.api.*;
import org.uom.cse.distributed.peer.utils.HashUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.uom.cse.distributed.Constants.ADDRESSES_PER_CHARACTER;
import static org.uom.cse.distributed.Constants.ADDRESS_SPACE_SIZE;
import static org.uom.cse.distributed.Constants.FILE_NAME_ARRAY;
import static org.uom.cse.distributed.Constants.GRACE_PERIOD_MS;
import static org.uom.cse.distributed.Constants.HEARTBEAT_FREQUENCY_MS;
import static org.uom.cse.distributed.Constants.HEARTBEAT_INITIAL_DELAY;
import static org.uom.cse.distributed.Constants.MAX_FILE_COUNT;
import static org.uom.cse.distributed.Constants.MIN_FILE_COUNT;
import static org.uom.cse.distributed.peer.api.State.CONFIGURED;
import static org.uom.cse.distributed.peer.api.State.CONNECTED;
import static org.uom.cse.distributed.peer.api.State.CONNECTING;
import static org.uom.cse.distributed.peer.api.State.IDLE;
import static org.uom.cse.distributed.peer.api.State.REGISTERED;

/**
 * The class to represent a Node in the distributed network.
 *
 * @author Imesha Sudasingha
 * @author Keet Sugathadasa
 */
public class Node implements RoutingTableListener {

    private static final Logger logger = LoggerFactory.getLogger(Node.class);

    private final StateManager stateManager = new StateManager(IDLE);
    private final RoutingTable routingTable = new RoutingTable();
    private final EntryTable entryTable = new EntryTable();
    private final List<String> myFiles = new ArrayList<>();
    private final UDPQuery udpQuery = new UDPQuery();

    private final CommunicationProvider communicationProvider;
    private final NodeServer server;
    private final String username;
    private final String ipAddress;
    private final int port;

    private Map<Integer, Map<Character, Map<String, List<EntryTableEntry>>>> predecessorEntries = new HashMap<>();
    private int nodeId;
    private char myChar;
    private ScheduledExecutorService executorService;
    private ScheduledFuture<?> periodicTask;

    private BootstrapProvider bootstrapProvider = new UDPBootstrapProvider();

    public Node(int port) {
        this(port, new UDPCommunicationProvider(), new UDPServer(port));
    }

    public Node(int port, CommunicationProvider communicationProvider, NodeServer server) {
        this(port, "localhost", communicationProvider, server);
    }

    public Node(int port, String ipAddress, CommunicationProvider communicationProvider, NodeServer server) {
        this(port, ipAddress, UUID.randomUUID().toString(), communicationProvider, server);
    }

    public Node(int port, String ipAddress, String username, CommunicationProvider communicationProvider, NodeServer server) {
        this.port = port;
        this.ipAddress = ipAddress;
        this.username = username;
        this.communicationProvider = communicationProvider;
        this.server = server;
    }

    public void start() {
        stateManager.checkState(State.IDLE);

        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));

        executorService = Executors.newScheduledThreadPool(2);
        routingTable.addListener(this);

        server.start(this);
        communicationProvider.start(this);
        udpQuery.initialize(this);

        logger.debug("Connecting to the distributed network");
        while (!stateManager.isState(CONNECTING)) {
            if (stateManager.isState(REGISTERED)) {
                unregister();
            }

            List<InetSocketAddress> peers = register();

            if (stateManager.isState(REGISTERED)) {
                Set<RoutingTableEntry> entries = connect(peers);
                // peer size become 0 only when we registered successfully
                if (peers.size() == 0 || entries.size() > 0) {
                    this.updateRoutingTable(entries);
                    stateManager.setState(CONNECTING);
                    logger.info("Successfully connected to the network and created routing table");
                }
            }
        }

        // 1. Select a Node Name
        this.nodeId = selectNodeName();
        logger.info("Selected node ID -> {}", this.nodeId);

        // 2. Add my node to my routing table
        routingTable.addEntry(new RoutingTableEntry(new InetSocketAddress(ipAddress, port), this.nodeId));
        logger.info("My routing table is -> {}", routingTable.getEntries());
        stateManager.setState(State.CONNECTED);

        // 3. Select my character
        myChar = HashUtils.nodeIdToChar(this.nodeId);
        logger.info("My char is -> {}", myChar);

        configure();
        stateManager.setState(CONFIGURED);

        // TODO: 10/24/17 Periodic synchronization
        /*
         * 1. Find 2 predecessors of mine.
         * 2. Then periodically ping them and synchronize with their entry tables.
         */
        periodicTask = executorService.scheduleAtFixedRate(() -> {
            try {
                runPeriodically();
            } catch (Exception e) {
                logger.error("Error occurred when running periodic check", e);
            }
        }, HEARTBEAT_INITIAL_DELAY, HEARTBEAT_FREQUENCY_MS, TimeUnit.MILLISECONDS);
    }

    private void configure() {
        // Calculate my characters
        Optional<RoutingTableEntry> myPredecessor = routingTable.findPredecessorOf(this.nodeId);
        logger.debug("My predecessor is -> {}", myPredecessor);
        Set<Character> characters = HashUtils.findCharactersOf(this.nodeId,
                myPredecessor.map(RoutingTableEntry::getNodeId).orElse(this.nodeId));
        characters.add(myChar);
        characters.forEach(entryTable::addCharacter);

        // TODO: 11/1/17 If majority fails in this operation, we need to retry entirely?
        // 5. Broadcast that I have joined the network to all entries in the routing table
        this.routingTable.getEntries().parallelStream()
                .filter(entry -> entry.getNodeId() != this.nodeId)
                .forEach(entry -> {
                    Map<Character, Map<String, List<EntryTableEntry>>> toBeUndertaken =
                            communicationProvider.notifyNewNode(
                                    entry.getAddress(), new InetSocketAddress(ipAddress, port), this.nodeId);
                    this.takeOverEntries(toBeUndertaken);
                });

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
                if (entry.isPresent() && entry.get().getNodeId() != this.nodeId) {
                    logger.info("Offering keyword ({}-{}) to Node -> {}", keyword, file, entry.get());

                    // Couldn't notify the actual owner. Keeping with myself
                    if (!communicationProvider.offerFile(entry.get().getAddress(), keyword, this.nodeId, file)) {
                        logger.warn("Unable to offer file ({} -> {}) to node -> {}. Keeping with me",
                                keyword, file, entry.get());
                        entryTable.addEntry(keyword, new EntryTableEntry(String.valueOf(this.nodeId), file));
                    }
                } else {
                    // I should take over this file name
                    logger.debug("I'm indexing ({}-{})", keyword, file);
                    entryTable.addEntry(keyword, new EntryTableEntry(String.valueOf(this.nodeId), file));
                }
            });
        });
    }

    private void runPeriodically() {
        // 1. Select 2 predecessors of mine
        Optional<RoutingTableEntry> predecessor1 = routingTable.findPredecessorOf(this.nodeId);
        logger.info("Pinging my predecessor for status -> {}", predecessor1);

        if (predecessor1.isPresent()) {
            RoutingTableEntry p1 = predecessor1.get();
            Optional<RoutingTableEntry> predecessor2 = routingTable.findPredecessorOf(p1.getNodeId());

            Map<Character, Map<String, List<EntryTableEntry>>> toBeHandedOver = this.getEntriesToHandoverTo(p1.getNodeId());
            logger.info("Handing over {} to -> {}", toBeHandedOver, p1.getNodeId());
            Map<Character, Map<String, List<EntryTableEntry>>> entries = communicationProvider.ping(p1.getAddress(), toBeHandedOver);

            boolean immediateSuccessorDead = false;
            if (entries == null) {
                logger.warn("My immediate successor -> {} is dead. Taking over", p1);
                immediateSuccessorDead = true;
                this.removeNode(p1.getAddress());
                this.entryTable.addAll(this.predecessorEntries.get(predecessor1.get().getNodeId()));
            } else {
                logger.debug("Found my immediate successor -> {}", p1);
                this.removeEntries(toBeHandedOver.keySet());
                this.predecessorEntries.put(predecessor1.get().getNodeId(), entries);
            }

            logger.info("Pinging SECOND immediate predecessor -> {}", predecessor2);
            if (predecessor2.isPresent() && predecessor2.get().getNodeId() != this.nodeId) {
                RoutingTableEntry p2 = predecessor2.get();

                toBeHandedOver = this.getEntriesToHandoverTo(p2.getNodeId());
                logger.info("Handing over {} to -> {}", toBeHandedOver, p2.getNodeId());
                entries = communicationProvider.ping(p2.getAddress(), toBeHandedOver);
                if (entries == null) {
                    logger.warn("My SECOND immediate successor -> {} is dead", p2);
                    this.removeNode(p2.getAddress());

                    if (immediateSuccessorDead) {
                        logger.info("Taking over {} which is my SECOND immediate successor", p2);
                        this.entryTable.addAll(this.predecessorEntries.get(p2.getNodeId()));
                    }
                } else {
                    logger.debug("Found my SECOND immediate successor -> {}", p2);
                    this.removeEntries(toBeHandedOver.keySet());
                    this.predecessorEntries.put(predecessor2.get().getNodeId(), entries);
                }
            } else {
                logger.warn("No SECOND immediate predecessor is present");
            }
        } else {
            logger.warn("No immediate predecessor is present");
        }
    }

    /**
     * Register and fetch 2 random peers from Bootstrap Server. Also retries until registration becomes successful.
     *
     * @return peers sent from Bootstrap server
     */
    private List<InetSocketAddress> register() {
        logger.debug("Registering node");
        List<InetSocketAddress> peers = null;
        try {
            peers = bootstrapProvider.register(ipAddress, port, username);
        } catch (IOException e) {
            logger.error("Error occurred when registering node", e);
        }

        if (peers == null) {
            logger.warn("Peers are null");
        } else {
            stateManager.setState(REGISTERED);
            logger.info("Node ({}:{}) registered successfully. Peers -> {}", ipAddress, port, peers);
        }

        return peers;
    }

    /**
     * Connect to the peers send by BS and fetch their routing tables. This method will later be reused for
     * synchronization purposes.
     *
     * @param peers peers to be connected
     * @return true if connecting successful and got at least one entry
     */
    private Set<RoutingTableEntry> connect(List<InetSocketAddress> peers) {
        logger.debug("Collecting routing table from peers: {}", peers);
        Set<RoutingTableEntry> entries = new HashSet<>();
        for (InetSocketAddress peer : peers) {
            Set<RoutingTableEntry> received = communicationProvider.connect(peer);
            logger.debug("Received routing table: {} from -> {}", received, peer);
            if (received.size() == 0) {
                logger.error("Failed to obtain routing table from -> {}", peer);
                entries.clear();
                break;
            }

            entries.addAll(received);
        }
        return entries;
    }

    /**
     * Unregister
     */
    private void unregister() {
        try {
            bootstrapProvider.unregister(ipAddress, port, username);
            stateManager.setState(IDLE);
            logger.debug("Unregistered from Bootstrap Server");
        } catch (IOException e) {
            logger.error("Error occurred when unregistering", e);
        }
    }

    /**
     * Selects a Node Name for the newly connected node (this one). When selecting, we chose a random node name within
     * <strong>1 - 180</strong> which maps from <strong>[A-Z0-9] -> [1-180]</strong>.
     *
     * @return The selected node name
     */
    private int selectNodeName() {
        Set<Integer> usedNodes = this.routingTable.getEntries().stream()
                .map(entry -> entry.getNodeId() / ADDRESSES_PER_CHARACTER)
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
            int fileCount = random.nextInt(MAX_FILE_COUNT - MIN_FILE_COUNT + 1) + MIN_FILE_COUNT;

            List<String> tempList = Arrays.asList(FILE_NAME_ARRAY);
            Collections.shuffle(tempList);
            return tempList.subList(0, fileCount);
        }
        return myFiles;
    }

    /**
     * Removes the given entry from the routing table
     *
     * @param node IP and port of the node to be removed
     */
    public void removeNode(InetSocketAddress node) {
        stateManager.checkState(CONNECTED, IDLE, CONFIGURED);
        logger.warn("Attempting to remove routing table entry -> {} from routing table", node);
        this.routingTable.removeEntry(node);
    }

    public void addNewNode(String ipAddress, int newNodePort, int newNodeId) {
        stateManager.checkState(State.CONNECTED, State.CONFIGURED);
        InetSocketAddress inetSocketAddress = new InetSocketAddress(ipAddress, newNodePort);
        RoutingTableEntry routingTableEntry = new RoutingTableEntry(inetSocketAddress, newNodeId);
        routingTable.addEntry(routingTableEntry);
    }


    /**
     * Returns the entries to be handed over to the newNode when a new node comes. This is based on the predecessor
     * relationship
     *
     * @param nodeId new node's ID
     * @return entries to be handed over
     */
    public Map<Character, Map<String, List<EntryTableEntry>>> getEntriesToHandoverTo(int nodeId) {
        stateManager.checkState(State.CONFIGURED, State.CONNECTED);

        // Find the predecessor of the node given
        Optional<RoutingTableEntry> entryOptional = routingTable.findPredecessorOf(nodeId);
        if (!entryOptional.isPresent()) {
            logger.warn("No predecessor found for node -> {}", nodeId);
            return null;
        }

        // 2. Now find the characters which should be handled by the new node. i.e: From its predecessor to new node
        RoutingTableEntry predecessor = entryOptional.get();
        logger.debug("Found predecessor {} for node -> {}", predecessor, nodeId);
        Set<Character> characters = HashUtils.findCharactersOf(nodeId, predecessor.getNodeId());
        // Adding the new node as well.
        characters.add(HashUtils.nodeIdToChar(nodeId));

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

    /**
     * Adds the entries given in <pre>toBeUndertaken</pre> into my {@link #entryTable}
     *
     * @param toBeUndertaken entries to be taken over
     */
    public void takeOverEntries(Map<Character, Map<String, List<EntryTableEntry>>> toBeUndertaken) {
        logger.debug("Undertaking entries -> {}", toBeUndertaken);

        toBeUndertaken.forEach((letter, keywordMap) -> {
            logger.debug("Undertaking letter [{}] and keywords -> {}", letter, keywordMap);

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
    }

    /**
     * Updates the {@link #routingTable} with entries coming from another node's routing table
     *
     * @param entries routing table entries received from another node
     */
    public void updateRoutingTable(Set<RoutingTableEntry> entries) {
        logger.debug("Adding routing table entries -> {}", entries);
        // TODO: 11/2/17 Should we sync? Remove what is not present?
        entries.forEach(routingTable::addEntry);
    }

    public void removeEntries(Set<Character> characters) {
        stateManager.checkState(State.CONNECTED, State.CONFIGURED);
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

            unregister();
        }

        communicationProvider.stop();
        server.stop();
        routingTable.removeListener(this);

        logger.debug("Shutting down periodic tasks");
        periodicTask.cancel(true);
        executorService.shutdownNow();
        try {
            executorService.awaitTermination(GRACE_PERIOD_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }

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

    public int getNodeId() {
        return nodeId;
    }

    public char getMyChar() {
        return myChar;
    }

    public UDPQuery getUdpQuery() {
        return udpQuery;
    }

    @Override
    public void entryAdded(RoutingTableEntry entry) {
        //        executorService.submit(this::configure);
    }

    @Override
    public void entryRemoved(RoutingTableEntry entry) {
        //        executorService.submit(this::configure);
    }
}