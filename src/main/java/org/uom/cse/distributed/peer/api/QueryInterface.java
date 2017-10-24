package org.uom.cse.distributed.peer.api;

import org.uom.cse.distributed.peer.Node;

import java.net.InetSocketAddress;

/**
 * This interface will provide the necessary structure for the file querying system.
 *
 * @author Keet Sugathadasa
 */
public interface QueryInterface {

    /**
     * This looks at the system and returns the nodes that contain  the file the node is looking for
     *
     * @param fileName the file name that needs to be searched in the network
     * @return InetSocketAddress list of the nodes that contain the file
     */
    public InetSocketAddress[] searchFullFile(String fileName);

    /**
     * this method looks at the files in the the node itself and return the node file
     *
     * @param fileName the file name that needs to be searched in the network
     * @return a boolean mentioning whether the file is contained within the node or not
     */
    public boolean searchMyFilesFullName(String fileName);
}
