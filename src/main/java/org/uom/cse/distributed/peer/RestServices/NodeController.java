package org.uom.cse.distributed.peer.RestServices;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uom.cse.distributed.peer.Node;
import org.uom.cse.distributed.peer.api.EntryTableEntry;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/nodecontroller")
public class NodeController {

    private static final Logger logger = LoggerFactory.getLogger(NodeController.class);
    private Node node;

    @GET
    @Path("/RouteTable")
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
    @Path("/NewNode/{id}")
    @Produces(MediaType.TEXT_PLAIN)
    public Response NewNode(@PathParam("id") int id) {
        try {
            return Response.ok(node.getEntriesToHandoverTo(id)).build();
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
