/* 
 * <Paste your header here>
 */
package org.uom.cse.distributed.peer.api.exceptions;

/**
 * An exception to be thrown when an error occurred at the routing level.
 *
 * @author Imesha Sudasingha
 */
public class RoutingException extends RuntimeException {

    public RoutingException(String message) {
        super(message);
    }

    public RoutingException(String message, Throwable cause) {
        super(message, cause);
    }
}
