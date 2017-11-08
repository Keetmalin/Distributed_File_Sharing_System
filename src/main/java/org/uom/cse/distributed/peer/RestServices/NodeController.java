package org.uom.cse.distributed.peer.RestServices;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uom.cse.distributed.peer.Node;
import org.uom.cse.distributed.peer.api.EntryTableEntry;
import org.uom.cse.distributed.peer.utils.RequestUtils;

import javax.ws.rs.*;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.uom.cse.distributed.Constants.SYNC_MSG_FORMAT;
import static org.uom.cse.distributed.Constants.TYPE_ENTRIES;

@Path("/nodecontroller")
public class NodeController {

    private static final Logger logger = LoggerFactory.getLogger(NodeController.class);
    private Node node;

    public NodeController(Node node) {
        this.node = node;
    }

    @GET
    @Path("/getRoutingTable")
    @Produces(MediaType.TEXT_PLAIN)
    public Response RouteTable() {
        try {
            return Response.ok(node.getRoutingTable().getEntries()).build();
        } catch (Exception e) {
            logger.error("Error occurred when building object request: {}", e);
            return Response.status(500).build();
        }
    }

    @GET
    @Path("/notifyNewNode/{ip}/{port}/{id}")
    @Produces(MediaType.TEXT_PLAIN)
    public Response NewNode(@PathParam("ip") String ip, @PathParam("port") int port, @PathParam("id") int id) {
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
    @Produces(MediaType.TEXT_PLAIN)
    public Response NewEntry(EntryTableEntry entry, @PathParam("keyWord") String key) {
        node.getEntryTable().addEntry(key, entry);
        return Response.status(200).build();
    }


    @GET
    @Path("/Query/{keyWord}")
    @Produces(MediaType.TEXT_PLAIN)
    public Response Query(@PathParam("keyWord") String key) {
        return Response.ok(node.getEntryTable().getEntriesByKyeword(key)).build();
    }
}
