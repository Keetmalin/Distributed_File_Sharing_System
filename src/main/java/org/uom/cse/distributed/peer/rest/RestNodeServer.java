package org.uom.cse.distributed.peer.rest;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uom.cse.distributed.peer.Node;
import org.uom.cse.distributed.peer.api.NodeServer;

import java.util.concurrent.ExecutorService;

import static org.uom.cse.distributed.Constants.RETRIES_COUNT;

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
        this.node = node;
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        jettyServer = new Server(node.getPort());
        jettyServer.setHandler(context);

        NodeController nodeController = new NodeController();

        ResourceConfig config = new ResourceConfig();
        config.register(nodeController);
        ServletContainer servletContainer = new ServletContainer(config);
        ServletHolder jerseyServlet = new ServletHolder(servletContainer);
        jerseyServlet.setInitOrder(0);

        // Tells the Jersey Servlet which REST service/class to load.
        jerseyServlet.setInitParameter("jersey.config.server.provider.classnames",
                NodeController.class.getCanonicalName());

        try {
            jettyServer.start();
        } catch (Exception e) {
            logger.error("Error occurred when starting REST server due to : {}", e.getMessage());
        }

        logger.info("REST Server started successfully ...");
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
