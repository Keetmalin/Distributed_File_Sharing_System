/*
 * Copyright to Eduze@UoM 2017
 */

package org.uom.cse.distributed.peer.api;

/**
 * The enum to represent the state of a node at a given time.
 *
 * @author Imesha Sudasingha
 */
public enum State {
    /** Program hasn't started yet */
    STOPPED,
    /** Program is starting */
    STARTING,
    /** Program has started, but hasn't done anything yet. */
    STARTED,
    /** Registered in the bootstrap server. That means, we have got 2 nodes (max) to connect to */
    REGISTERED,
    /** Connected to first 2 peers and response arrived along with routing tables, etc */
    CONNECTED
}
