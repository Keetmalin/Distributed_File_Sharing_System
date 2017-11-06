package org.uom.cse.distributed.peer.rest;


import org.glassfish.jersey.client.JerseyClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uom.cse.distributed.peer.Node;
import org.uom.cse.distributed.peer.api.CommunicationProvider;
import org.uom.cse.distributed.peer.api.EntryTableEntry;
import org.uom.cse.distributed.peer.api.RoutingTableEntry;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Created by Vithusha on 10/24/2017.
 */
public class RestCommunicationProvider extends CommunicationProvider {
    /** Logger to log the events. */
    private static final Logger logger = LoggerFactory.getLogger(RestCommunicationProvider.class);

    private Node node;

    @Override
    public void start(Node node) {
        this.node = node;
        //this.queryHopCount = 1;
        logger.info("Communication provider started");

    }

    @Override
    public void stop() {

    }

    //    @Override
    //    public Set<RoutingTableEntry> connect(InetSocketAddress peer) {
    //        String ipAddress = peer.getHostName();
    //        int port = peer.getPort();
    //        String stringURL = "http://" + ipAddress + ":" + String.valueOf(port) + "/getRoutingTable";
    //        HashSet<RoutingTableEntry> set = new HashSet<>();
    //        javax.ws.rs.client.Client client = JerseyClientBuilder.createClient();
    //        try {
    //            ClientResponse response = getResponse(stringURL);
    //            if (response != null) {
    //                Gson gson = new Gson();
    //                Type type = new TypeToken<HashSet<RoutingTableEntry>>() {
    //                }.getType();
    //                set = gson.fromJson(getStringResponse(response), type);
    //            }
    //
    //        } catch (Exception e) {
    //            LOGGER.error("Error occurred when obtaining routing table", e);
    //        }
    //        return set;
    //    }
    @Override
    public Set<RoutingTableEntry> connect(InetSocketAddress peer) {
        UriBuilder url = UriBuilder.fromPath("getRoutingTable")
                .scheme("http")
                .host(peer.getAddress().getHostAddress())
                .port(peer.getPort());

        Client client = JerseyClientBuilder.createClient();
        try {
            return client.target(url)
                    .request(MediaType.APPLICATION_JSON)
                    .get(new GenericType<Set<RoutingTableEntry>>() {});
        } catch (Exception e) {
            logger.error("error occurred when connecting {} : {}", url, e);
        } finally {
            client.close();
        }

        return new HashSet<>();
    }

    @Override
    public boolean disconnect(InetSocketAddress peer) {
        return false;
    }

    @Override
    public Map<Character, Map<String, List<EntryTableEntry>>> notifyNewNode(InetSocketAddress peer, InetSocketAddress me, int nodeId) {
        UriBuilder url = UriBuilder.fromPath("NotifyNewNode")
                .path(me.getAddress().getHostAddress())
                .path(String.valueOf(me.getPort()))
                .path(String.valueOf(nodeId))
                .scheme("http")
                .host(peer.getAddress().getHostAddress())
                .port(peer.getPort());

        Client client = JerseyClientBuilder.createClient();
        try {
            logger.debug("Notifying new node to {} as message: {}", peer, url);
            return client.target(url)
                    .request(MediaType.APPLICATION_JSON)
                    .get(new GenericType<Map<Character, Map<String, List<EntryTableEntry>>>>() {});
        } catch (Exception e) {
            logger.error("Error occurred when notifying new node to -> {} with message : {}", url, e);
        } finally {
            client.close();
        }

        return new HashMap<>();
    }

    @Override
    public boolean offerFile(InetSocketAddress peer, String keyword, int node, String file) {
        UriBuilder url = UriBuilder.fromPath("NewEntry")
                .path(keyword)
                .scheme("http")
                .host(peer.getAddress().getHostAddress())
                .port(peer.getPort());

        EntryTableEntry entry = new EntryTableEntry(String.valueOf(node), file);
        Client client = JerseyClientBuilder.createClient();
        try {
            logger.debug("Offering file to {} as message: {}", peer, url);
            client.target(url)
                    .request(MediaType.APPLICATION_JSON)
                    .post(Entity.json(entry));
            return true;
        } catch (Exception e) {
            logger.error("Error occurred when notifying new node to -> {} with message : {}", url, e);
        } finally {
            client.close();
        }

        return false;
    }

    @Override
    public Set<InetSocketAddress> searchFullFile(InetSocketAddress targetNode, String fileName, String keyword) {
        UriBuilder url = UriBuilder.fromPath("Query")
                .path(keyword)
                .scheme("http")
                .host(targetNode.getAddress().getHostAddress())
                .port(targetNode.getPort());

        Client client = JerseyClientBuilder.createClient();
        try {
            logger.debug("Querying keyword {} in {}", keyword, url);
            List<EntryTableEntry> entries = client.target(url)
                    .request(MediaType.APPLICATION_JSON)
                    .get(new GenericType<List<EntryTableEntry>>() {});

            // TODO: 11/7/17 Should we return a set of InetSocketAddresses?
        } catch (Exception e) {
            logger.error("Error occurred when notifying new node to -> {} with message : {}", url, e);
        } finally {
            client.close();
        }

        return null;
    }

    @Override
    public Map<Character, Map<String, List<EntryTableEntry>>> ping(InetSocketAddress peer,
            Map<Character, Map<String, List<EntryTableEntry>>> toBeHandedOver) {
        return null;
    }
}
