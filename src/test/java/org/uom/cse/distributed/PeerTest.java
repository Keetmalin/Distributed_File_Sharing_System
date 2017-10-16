/*
 * Copyright to Eduze@UoM 2017
 */

package org.uom.cse.distributed;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.uom.cse.distributed.server.BootstrapServer;

public class PeerTest {

    private static BootstrapServer bootstrapServer;

    @BeforeClass
    public void setUp() {
        bootstrapServer = new BootstrapServer(Constants.BOOTSTRAP_PORT);
        bootstrapServer.start();
    }

    @AfterClass
    public void tearDown() {
        bootstrapServer.stop();
    }
}
