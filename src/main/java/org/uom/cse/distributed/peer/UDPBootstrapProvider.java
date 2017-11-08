/* 
 * <Paste your header here>
 */
package org.uom.cse.distributed.peer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uom.cse.distributed.Constants;
import org.uom.cse.distributed.peer.api.BootstrapProvider;
import org.uom.cse.distributed.peer.utils.RequestUtils;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.List;

import static org.uom.cse.distributed.Constants.BOOTSTRAP_IP;
import static org.uom.cse.distributed.Constants.BOOTSTRAP_PORT;
import static org.uom.cse.distributed.Constants.REG_MSG_FORMAT;
import static org.uom.cse.distributed.Constants.RETRIES_COUNT;

/**
 * Connects to bootstrap server and register using UDP packets
 *
 * @author Imesha Sudasingha
 */
public class UDPBootstrapProvider implements BootstrapProvider {

    private static final Logger logger = LoggerFactory.getLogger(UDPBootstrapProvider.class);

    private final int numOfRetries = RETRIES_COUNT;

    public UDPBootstrapProvider() { }

    /** {@inheritDoc} */
    @Override
    public List<InetSocketAddress> register(String ipAddress, int port, String username) throws SocketException {
        String msg = String.format(REG_MSG_FORMAT, ipAddress, port, username);
        String request = RequestUtils.buildRequest(msg);

        int retriesLeft = numOfRetries;
        while (retriesLeft > 0) {
            try (DatagramSocket datagramSocket = new DatagramSocket()) {
                String response = RequestUtils.sendRequest(datagramSocket, request, InetAddress.getByName(BOOTSTRAP_IP),
                        BOOTSTRAP_PORT);
                logger.debug("Response received : {}", response);
                return RequestUtils.processRegisterResponse(response);
            } catch (IOException e) {
                logger.error("Error occurred when sending the request", e);
                retriesLeft--;
            }
        }

        return null;
    }

    /** {@inheritDoc} */
    @Override
    public boolean unregister(String ipAddress, int port, String username) throws SocketException {
        logger.debug("Unregistering node");

        String msg = String.format(Constants.UNREG_MSG_FORMAT, ipAddress, port, username);
        String request = RequestUtils.buildRequest(msg);

        int retriesLeft = numOfRetries;
        while (retriesLeft > 0) {
            try (DatagramSocket datagramSocket = new DatagramSocket()) {
                String response = RequestUtils.sendRequest(datagramSocket, request,
                        InetAddress.getByName(Constants.BOOTSTRAP_IP), Constants.BOOTSTRAP_PORT);
                logger.debug("Response received : {}", response);
                if (RequestUtils.processUnregisterResponse(response)) {
                    logger.info("Successfully unregistered");
                    return true;
                } else {
                    logger.warn("Unable to unregister");
                    return false;
                }
            } catch (IOException e) {
                logger.error("Error occurred when sending the request", e);
                retriesLeft--;
            }
        }

        return false;
    }

}
