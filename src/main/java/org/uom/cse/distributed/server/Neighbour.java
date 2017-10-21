package org.uom.cse.distributed.server;

/**
 * A bean to represent a node in the distributed network. {@link BootstrapServer} uses this.
 *
 * Taken from CS4262 course CSE
 * Provided by Dilum Bandara and Anura P. Jayasumana
 */
class Neighbour {
    private String ip;
    private int port;
    private String username;

    public Neighbour(String ip, int port, String username) {
        this.ip = ip;
        this.port = port;
        this.username = username;
    }

    public String getIp() {
        return this.ip;
    }

    public String getUsername() {
        return this.username;
    }

    public int getPort() {
        return this.port;
    }
}
