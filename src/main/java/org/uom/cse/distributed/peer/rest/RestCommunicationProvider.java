package org.uom.cse.distributed.peer.rest;

import org.glassfish.jersey.client.JerseyClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uom.cse.distributed.peer.Node;
import org.uom.cse.distributed.peer.api.CommunicationProvider;
import org.uom.cse.distributed.peer.api.EntryTableEntry;
import org.uom.cse.distributed.peer.api.RoutingTableEntry;
import org.uom.cse.distributed.peer.utils.RequestUtils;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
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
    public void stop() { }

    @Override
    public boolean disconnect(InetSocketAddress peer) {
        return false;
    }

    @SuppressWarnings("unchecked")
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
            String response = client.target(url)
                    .request(MediaType.APPLICATION_JSON)
                    .get(String.class);
            if (response != null) {
                Object obj = RequestUtils.base64StringToObject(response);
                logger.debug("Received entries to take over -> {}", obj);
                if (obj != null) {
                    return (Map<Character, Map<String, List<EntryTableEntry>>>) obj;
                }
            }
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
                    .post(Entity.json(RequestUtils.buildObjectRequest(entry)));
            return true;
        } catch (Exception e) {
            logger.error("Error occurred when offering keyword -> {} to -> {} with message : {}", keyword, url, e);
        } finally {
            client.close();
        }

        return false;
    }

    @SuppressWarnings("unchecked")
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
            String response = client.target(url)
                    .request(MediaType.APPLICATION_JSON)
                    .get(String.class);

            logger.debug("Received response : {}", response);
            if (response != null) {
                Object obj = RequestUtils.base64StringToObject(response);
                logger.debug("Received entries for query {} -> {}", keyword, obj);
                if (obj != null) {
                    return (Set<InetSocketAddress>) obj;
                }
            }
        } catch (Exception e) {
            logger.error("Error occurred when notifying new node to -> {} with message : {}", url, e);
        } finally {
            client.close();
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Set<String> searchKeywordFile(InetSocketAddress targetNode, String keyword) {
        UriBuilder url = UriBuilder.fromPath("Keyword")
                .path(keyword)
                .scheme("http")
                .host(targetNode.getAddress().getHostAddress())
                .port(targetNode.getPort());

        Client client = JerseyClientBuilder.createClient();
        try {
            logger.debug("Querying keyword {} in {}", keyword, url);
            String response = client.target(url)
                    .request(MediaType.APPLICATION_JSON)
                    .get(String.class);

            logger.debug("Received response : {}", response);
            if (response != null) {
                Object obj = RequestUtils.base64StringToObject(response);
                logger.debug("Received entries for query {} -> {}", keyword, obj);
                if (obj != null) {
                    return (Set<String>) obj;
                }
            }
        } catch (Exception e) {
            logger.error("Error occurred when notifying new node to -> {} with message : {}", url, e);
        } finally {
            client.close();
        }

        return  null;
    }

    @Override
    public Map<Character, Map<String, List<EntryTableEntry>>> ping(InetSocketAddress peer,
            Map<Character, Map<String, List<EntryTableEntry>>> toBeHandedOver) {
        String base64 = null;
        try {
            base64 = RequestUtils.buildObjectRequest(toBeHandedOver);
        } catch (IOException e) {
            logger.error("Error occurred when encoding entries to be handed over to -> {}", peer, e);
            throw new IllegalArgumentException("Unable to encode entries", e);
        }

        UriBuilder url = UriBuilder.fromPath("Ping")
                .path(String.valueOf(this.node.getNodeId()))
                .path(base64)
                .scheme("http")
                .host(peer.getAddress().getHostAddress())
                .port(peer.getPort());

        Client client = JerseyClientBuilder.createClient();
        try {
            logger.debug("Pinging {} at {}", peer, url);
//            Response response = client.target(url)
//                    .request(MediaType.APPLICATION_JSON_TYPE)
//                    .post(Entity.json(RequestUtils.buildObjectRequest(base64)));
            String response = client.target(url)
                    .request(MediaType.APPLICATION_JSON)
                    .get(String.class);
            logger.debug("Received response : {}", "");
            if (response != null) {
                Object obj = RequestUtils.base64StringToObject(response);
                logger.debug("Received entries to take over -> {}", obj);
                if (obj != null) {
                    return (Map<Character, Map<String, List<EntryTableEntry>>>) obj;
                }
            }
        } catch (Exception e) {
            logger.error("Error occurred when connecting to -> {} with message : {}", url, e);
        } finally {
            client.close();
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Set<RoutingTableEntry> connect(InetSocketAddress peer) {
        UriBuilder url = UriBuilder.fromPath("getRoutingTable")
                .scheme("http")
                .host(peer.getAddress().getHostAddress())
                .port(peer.getPort());

        Client client = JerseyClientBuilder.createClient();
        try {
            logger.debug("Connecting to {} at {}", peer, url);
            String response = client.target(url)
                    .request(MediaType.APPLICATION_JSON_TYPE)
                    .get(String.class);
            logger.debug("Received response : {}", response);
            if (response != null) {
                Object obj = RequestUtils.base64StringToObject(response);
                logger.debug("Received routing table -> {}", obj);
                if (obj != null) {
                    return (Set<RoutingTableEntry>) obj;
                }
            }
        } catch (Exception e) {
            logger.error("Error occurred when connecting to -> {} with message : {}", url, e);
        } finally {
            client.close();
        }

        return null;
    }
}