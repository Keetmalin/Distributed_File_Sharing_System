package org.uom.cse.distributed.GUI;

import javax.swing.JTextArea;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;

import org.uom.cse.distributed.peer.rest.RestCommunicationProvider;

import java.net.UnknownHostException;


class StatusMessageAppender extends AppenderSkeleton {

    static Logger logger = Logger.getLogger(StatusMessageAppender.class);
    private  JTextArea jTextA;

    public StatusMessageAppender(JTextArea textrea) {
        this.jTextA = textrea;
        logger.debug("I am getting printed on the terminal");


    }
    protected void append(LoggingEvent event)
    {
        if(event.getLevel().equals(Level.INFO)){
            jTextA.append(event.getMessage().toString());
        }
    }
    public void close()
    {
    }
    public boolean requiresLayout()
    {
        return false;
    }
}