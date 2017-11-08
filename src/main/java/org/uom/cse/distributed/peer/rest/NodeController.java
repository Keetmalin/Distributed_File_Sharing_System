package org.uom.cse.distributed.peer.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uom.cse.distributed.peer.Node;
import org.uom.cse.distributed.peer.UDPCommunicationProvider;
import org.uom.cse.distributed.peer.api.EntryTableEntry;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.uom.cse.distributed.Constants.*;

@Path("/")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class NodeController {

    private static final Logger logger = LoggerFactory.getLogger(NodeController.class);
    private Node node;
    private final int numOfRetries = RETRIES_COUNT;

    public NodeController(Node node) {
        this.node = node;
    }

    @GET
    @Path("/getRoutingTable")
    public Response routeTable() {
        try {
            return Response.ok(node.getRoutingTable().getEntries()).build();
        } catch (Exception e) {
            logger.error("Error occurred when building object request: {}", e);
            return Response.status(500).build();
        }
    }

    @GET
    @Path("/NotifyNewNode/{ip}/{port}/{id}")
    public Response newNode(@PathParam("ip") String ip, @PathParam("port") int port, @PathParam("id") int id) {
        try {
            node.addNewNode(ip, port, id);
            Map<Character, Map<String, List<EntryTableEntry>>> entriesToHandover = this.node.getEntriesToHandoverTo(id);
            if (entriesToHandover == null) {
                logger.warn("Couldn't find characters to be handed over to node -> {}", id);
                return Response.status(500).build();
            }
            // Send the entries to new node. Only if that's successful, we remove them from myself
            logger.debug("Notifying characters belonging to node -> {} : {}", id, entriesToHandover.keySet());
            //TODO : Imesha need to verify relevant client got the response
            node.removeEntries(entriesToHandover.keySet());
            return Response.ok(entriesToHandover).build();
        } catch (Exception e) {
            logger.error("Couldn't find characters to be handed over to node -> {}", id);
            return Response.status(500).build();
        }
    }

    @POST
    @Path("/NewEntry/{keyWord}")
    public Response newEntry(EntryTableEntry entry, @PathParam("keyWord") String key) {
        node.getEntryTable().addEntry(key, entry);
        return Response.status(200).build();
    }


    @GET
    @Path("/Query/{keyWord}")
    public Response query(@PathParam("keyWord") String key) {
        return Response.ok(node.getEntryTable().getEntriesByKyeword(key)).build();
    }

    @GET
    @Path("/Ping/{id}")
    public Response ping(Map<Character, Map<String, List<EntryTableEntry>>> toBeHandedOver, @PathParam("id") int id) {

        Map<Character, Map<String, List<EntryTableEntry>>> toBeTakenOver;
        logger.info("Received characters to be taken over -> {}", toBeHandedOver);

        if (toBeHandedOver != null) {
            toBeTakenOver = toBeHandedOver;
            this.node.takeOverEntries(toBeTakenOver);
        }
        int nodeId = id;

        // TODO: NEED to complete
        // 1. Send my entries to this node
        return Response.ok(node.getEntryTable().getEntries()).build();

        // TODO: 11/2/17 Add the calling node to my routing table if not present

//        // 2. Also send any characters to be taken over to this one as well. If present
//        Optional<RoutingTableEntry> tableEntryOptional = this.node.getRoutingTable().findByNodeId(nodeId);
//        if (tableEntryOptional.isPresent()) {
//            handoverEntries(nodeId, tableEntryOptional.get().getAddress());
//
//            // 3. Send my routing table to that node as well
//            provideRoutingTable(tableEntryOptional.get().getAddress());
//        }
//
//        return Response.ok().build();

    }

    @GET
    @Path("/Sync/{type}")
    public Response sync(Map<Character, Map<String, List<EntryTableEntry>>> obj, @PathParam("type") String type) {
        switch (type) {
            case TYPE_ENTRIES:
                logger.debug("Received characters to be taken over -> {}", obj);
                if (obj != null) {
                    Map<Character, Map<String, List<EntryTableEntry>>> toBeTakenOver = obj;
                    this.node.takeOverEntries(toBeTakenOver);
                }
                break;
            case TYPE_ROUTING:
                logger.debug("Received routing table -> {}", obj);
                break;
        }

        // retryOrTimeout(RESPONSE_OK, recipient);
        return Response.ok().build();
    }
}