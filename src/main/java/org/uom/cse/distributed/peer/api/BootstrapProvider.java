package org.uom.cse.distributed.peer.api;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;

/**
 * Interface to communicate with the bootstrap server.
 *
 * @author Imesha Sudasingha
 */
public interface BootstrapProvider {

    List<InetSocketAddress> register(String ipAddress, int port, String username) throws IOException;

    boolean unregister(String ipAddress, int port, String username) throws IOException;
}
