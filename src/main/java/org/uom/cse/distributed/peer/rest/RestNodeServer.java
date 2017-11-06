package org.uom.cse.distributed.peer.rest;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uom.cse.distributed.peer.Node;
import org.uom.cse.distributed.peer.api.NodeServer;

/**
 * This Class provides the implementation of the server side, of each of the nodes.
 *
 * @author Vithusha Aarabhi
 * @author Jayan Vidanapathirana
 */
public class RestNodeServer implements NodeServer {

    private static final Logger logger = LoggerFactory.getLogger(RestNodeServer.class);
    private boolean started = false;

    private Server jettyServer;

    @Override
    public void start(Node node) {
        if (started) {
            logger.warn("Listener already running");
        } else {
            ResourceConfig config = new ResourceConfig();
            config.register(new NodeController(node));
            ServletHolder servlet = new ServletHolder(new ServletContainer(config));

            jettyServer = new Server(node.getPort());

            ServletContextHandler context = new ServletContextHandler(jettyServer, null);
            context.addServlet(servlet, "/*");
            context.setSessionHandler(new SessionHandler());
            jettyServer.setHandler(context);

            try {
                jettyServer.start();
            } catch (Exception e) {
                logger.error("Error occurred when starting REST server due to", e);
                return;
            }

            logger.info("REST Server started successfully ...");
        }
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
