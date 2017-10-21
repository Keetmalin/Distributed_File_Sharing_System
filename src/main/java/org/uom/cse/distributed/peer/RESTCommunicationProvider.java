/* 
 * <Paste your header here>
 */
package org.uom.cse.distributed.peer;

import org.uom.cse.distributed.peer.api.CommunicationProvider;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * This the web services/ REST based implementation to communicate with the peers.
 */
public class RESTCommunicationProvider implements CommunicationProvider {

    @Override
    public List<RoutingTable.Entry> connect(InetSocketAddress peer) {
        return new ArrayList<>();
    }

    @Override
    public boolean disconnect(InetSocketAddress peer) {
        return true;
    }
}
