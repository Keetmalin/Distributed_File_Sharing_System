package org.uom.cse.distributed.peer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uom.cse.distributed.peer.api.CommunicationProvider;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * Provides UDP Socket Based communication with Peers
 *
 * @author Keet Sugathadasa
 */
public class UDPCommunicationProvider implements CommunicationProvider{

    private static final Logger logger = LoggerFactory.getLogger(Node.class);

    @Override
    public List<RoutingTable.Entry> connect(InetSocketAddress peer) {

        logger.debug("connecting to peer");
        return null;
    }

    @Override
    public boolean disconnect(InetSocketAddress peer) {
        return false;
    }
}
