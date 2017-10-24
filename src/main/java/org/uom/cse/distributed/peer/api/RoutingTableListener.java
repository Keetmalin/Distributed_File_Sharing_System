/* 
 * <Paste your header here>
 */
package org.uom.cse.distributed.peer.api;

/**
 * An interface to listen for routing table level changes
 *
 * @author Imesha Sudasingha
 */
public interface RoutingTableListener {

    void entryAdded(RoutingTableEntry entry);

    void entryRemoved(RoutingTableEntry entry);
}
