/* 
 * <Paste your header here>
 */
package org.uom.cse.distributed.peer;

import org.uom.cse.distributed.peer.api.CommunicationProvider;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * This is the socket based implementation of the communication provider. That is, "Phase 2" of the project.
 */
public class SocketBasedCommunicationProvider implements CommunicationProvider {

    @Override
    public List<RoutingTable.Entry> connect(InetSocketAddress peer) {
        return new ArrayList<>();
    }

    @Override
    public boolean disconnect(InetSocketAddress peer) {
        return true;
    }
}
