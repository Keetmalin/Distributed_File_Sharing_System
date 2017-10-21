package org.uom.cse.distributed;

/**
 * Constants
 */
public class Constants {

    private Constants() { }

    /** Need to set the bootstrap IP and Port here */
    public static final int BOOTSTRAP_PORT = 55555;
    public static final String BOOTSTRAP_IP = "127.0.0.1";

    /** How many times a given UDP request be retried */
    public static final int RETRIES_COUNT = 3;

    /** REG ${ip} ${port} ${username} */
    public static final String REG_MSG_FORMAT = "REG %s %d %s";

    /** UNREG ${ip} ${port} ${username} */
    public static final String UNREG_MSG_FORMAT = "UNREG %s %d %s";

    /** Message format to be used when sending a request to the bootstrap server. ${length} ${msg} */
    public static final String MSG_FORMAT = "%04d %s";
    public static final String REGOK = "REGOK";
    public static final String UNROK = "UNROK";

    /** Message format to be used in Client Server Communications **/
    public static final String GETROUTINGTABLE = "GETRT";
    public static final String BROADCAST = "BCAST";
    public static final String HANDOVER = "HNDVR";
}
