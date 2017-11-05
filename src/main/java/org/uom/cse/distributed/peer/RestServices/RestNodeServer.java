package org.uom.cse.distributed.peer.RestServices;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uom.cse.distributed.peer.Node;
import org.uom.cse.distributed.peer.api.EntryTableEntry;
import org.uom.cse.distributed.peer.api.NodeServer;
import org.uom.cse.distributed.peer.utils.RequestUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import static org.uom.cse.distributed.Constants.RETRIES_COUNT;
import static org.uom.cse.distributed.Constants.SYNC_MSG_FORMAT;
import static org.uom.cse.distributed.Constants.TYPE_ENTRIES;

/**
 * This Class provides the implementation of the server side, of each of the nodes.
 *
 * @author Vithusha Aarabhi
 * @author Jayan Vidanapathirana
 */
public class RestNodeServer implements NodeServer {

    private static final Logger logger = LoggerFactory.getLogger(RestNodeServer.class);
    private final int numOfRetries = RETRIES_COUNT;
    private ExecutorService executorService;
    private boolean started = false;

    private Node node;
    private Server jettyServer;

    @Override
    public void start(Node node) {
        if (started) {
            logger.warn("Listener already running");
        }else {
        this.node = node;

        ResourceConfig config = new ResourceConfig();
        config.packages("RestServices");
        ServletHolder servlet = new ServletHolder(new ServletContainer(config));

        jettyServer = new Server(node.getPort());
        ServletContextHandler context = new ServletContextHandler(jettyServer,"/*");
        context.addServlet(servlet,"/*");

        try {
            jettyServer.start();
//            jettyServer.join();
        } catch (Exception e) {
            logger.error("Error occurred when starting REST server due to : {}", e.getMessage());
        }

        logger.info("REST Server started successfully ...");
        }
    }



    private boolean handoverEntries(int nodeId, InetSocketAddress recipient) throws IOException {

        return false;
    }











    @Override
    public void stop() {
        if (started) {
            started = false;
            try {
                jettyServer.stop();
            } catch (Exception e) {
                logger.error("Error occurred when stopping the REST server due to : {}", e.getMessage());
            }
        }
    }

    @Override
    public void listen() {

    }


}
