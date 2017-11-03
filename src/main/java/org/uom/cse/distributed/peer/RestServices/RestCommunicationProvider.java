package org.uom.cse.distributed.peer.RestServices;



import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uom.cse.distributed.peer.Node;
import org.uom.cse.distributed.peer.api.CommunicationProvider;
import org.uom.cse.distributed.peer.api.EntryTableEntry;
import org.uom.cse.distributed.peer.api.RoutingTableEntry;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.util.*;


/**
 * Created by Vithusha on 10/24/2017.
 */


public class RestCommunicationProvider extends CommunicationProvider {
    /**
     * Logger to log the events.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(RestCommunicationProvider.class);

    // Create Jersey client

    ClientConfig clientConfig = new DefaultClientConfig();




    @Override
    public void start(Node node) {

    }

    @Override
    public void stop() {

    }


    @Override
    public Set<RoutingTableEntry> connect(InetSocketAddress peer) {
        String ipAddress = peer.getHostName();
        int port = peer.getPort();
        String stringURL = "http://" + ipAddress + ":" + String.valueOf(port) + "/getRoutingTable";
        HashSet<RoutingTableEntry> set = new HashSet<>();

        try {

            ClientResponse response = getResponse(stringURL);
            if (response != null) {
                Gson gson = new Gson();
                Type type = new TypeToken<HashSet<RoutingTableEntry>>() {
                }.getType();
                set =  gson.fromJson(getStringResponse(response), type);
            }

        } catch (Exception e) {
            LOGGER.error("Error occurred when obtaining routing table", e);
        }
        return set;
    }

    @Override
    public boolean disconnect(InetSocketAddress peer) {
        return false;
    }

    @Override
    public Map<Character, Map<String, List<EntryTableEntry>>> notifyNewNode(InetSocketAddress peer, InetSocketAddress me, int nodeId) {

        String peer_ipAddress = peer.getHostName();
        int peer_port = me.getPort();
        String my_ipAddress = peer.getHostName();
        int my_port = me.getPort();
        String stringURL = "http://" + peer_ipAddress + ":" + String.valueOf(peer_port) + "/getRoutingTable" + "/" + my_ipAddress + "/" + String.valueOf(my_port) + "/" + String.valueOf(nodeId);
        try {

            LOGGER.debug("Notifying new node to {} as message: {}", peer);
            ClientResponse response = getResponse(stringURL);
            String json_response = getStringResponse(response);
            LOGGER.debug("Received response : {}", json_response);
        } catch (IOException e) {
            LOGGER.debug("");
        }

        return new HashMap<>();
    }

    @Override
    public boolean offerFile(InetSocketAddress peer, String keyword, int node, String file) {

        String ipAddress = peer.getHostName();
        int port = peer.getPort();
        String stringURL = "http://" + ipAddress + ":" + String.valueOf(port) + "/getRoutingTable" + "/" + keyword + "/" + node + "/" + file;
        ClientResponse response = null;
        try {

            LOGGER.debug("Sending request to get the routing table from {}", peer);
            response = getResponse(stringURL);

            String json_response = getStringResponse(response);
            LOGGER.debug("Received response: {}", json_response);
        } catch (IOException e) {
            LOGGER.debug("");
        }
        return response != null;
    }

    @Override
    public Set<InetSocketAddress> searchFullFile(InetSocketAddress targetNode, String fileName, String keyword) {
        String ipAddress = targetNode.getHostName();
        int port = targetNode.getPort();
        String stringURL = "http://" + ipAddress + ":" + String.valueOf(port) + "/searchFile" + "/" + fileName +"/" + keyword;
        HashSet<InetSocketAddress> set = new HashSet<>();
        try {
            LOGGER.debug("Searching filename: {} , with keyword: {} in the network", fileName, keyword);
            ClientResponse response = getResponse(stringURL);
            LOGGER.debug("Received response ");
            if (response != null) {
                Gson gson = new Gson();
                Type type = new TypeToken<HashSet<InetSocketAddress>>() {
                }.getType();
                set =  gson.fromJson(getStringResponse(response), type);
            }

        } catch (Exception e) {
            LOGGER.error("Error occurred when obtaining routing table", e);
        }
        return set;
    }

    @Override
    public Map<Character, Map<String, List<EntryTableEntry>>> ping(InetSocketAddress peer, Map<Character, Map<String, List<EntryTableEntry>>> toBeHandedOver) {
        return null;
    }

    private ClientResponse getResponse(String url) {

        Client client = Client.create();
        WebResource webResource = client
                .resource(url);
        ClientResponse response = webResource.accept("application/json")
                .get(ClientResponse.class);
        if (response.getStatus() != 200) {
            throw new RuntimeException("Failed : HTTP error code : "
                    + response.getStatus());
        }
        return  response;
    }

    private String getStringResponse (ClientResponse response) throws IOException {

        if (response != null) {
            String json_response = "";
            InputStreamReader in = new InputStreamReader(response.getEntityInputStream());
            BufferedReader br = new BufferedReader(in);
            String text;

            while ((text = br.readLine()) != null) {
                json_response += text;
            }
            return json_response;
        }
        return null;
    }

}
