package org.uom.cse.distributed.peer.api;

import org.uom.cse.distributed.peer.Node;

import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * This is the interface to be used to listen to node requests in the network. Provides server methods for nodes in the
 * distributed system
 *
 * @author Keet Sugathadasa
 */

public interface NodeServer {

    /**
     * this starts a server on the node that is capable of listening to incoming requests
     *
     * @param node {@link Node} the node which needs to instantiate the server side
     */
    void start(Node node);

    /**
     * this stops a server on the node that is capable of listening to incoming request
     */
    void stop();

    /**
     * this starts listening to incoming packets from other nodes
     */
    void listen();

    /**
     * this gets the current nodes routing table and pass it down to whoever is requesting is
     *
     * @param recipient The client who sent the incoming packet
     */
//    void provideRoutingTable(InetSocketAddress recipient) throws IOException;
//
//    /**
//     * this gets the current nodes routing table and pass it down to whoever is requesting is
//     *
//     * @param request   Request received
//     * @param recipient The client who is sending the request
//     */
//    void handleNewNodeRequest(String request, InetSocketAddress recipient) throws IOException;
}
