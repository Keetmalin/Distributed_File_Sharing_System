package org.uom.cse.distributed.performance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uom.cse.distributed.peer.Node;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.uom.cse.distributed.Constants.FILE_NAME_ARRAY;
import static org.uom.cse.distributed.Constants.MAX_FILE_COUNT;
import static org.uom.cse.distributed.Constants.MIN_FILE_COUNT;

/**
 * @author Keet Sugathadasa
 */
public class PerformanceMeasure {

    private static final Logger logger = LoggerFactory.getLogger(Node.class);
    private Node node;

    public PerformanceMeasure(Node node){
        this.node = node;
    }

    public long calculateQueryLatency(int testCount) {

        long timeSum = 0;
        logger.info("Calculating Average latency for {} times" , testCount);

        Random random = new Random();

        for (int i = 0; i < testCount; i++) {
            int fileIndex = random.nextInt(FILE_NAME_ARRAY.length);

            String fileName = FILE_NAME_ARRAY[fileIndex];

            long startTime = System.currentTimeMillis();
            this.node.getUdpQuery().searchFullFile(fileName);

            long stopTime = System.currentTimeMillis();
            timeSum += stopTime - startTime;

        }
        return timeSum/testCount;
    }

    public float calculateQueryHopCount(int testCount){

        int countSum = 0;
        logger.info("Calculating Average Hop Count for {} times" , testCount);
        Random random = new Random();

        for (int i = 0 ; i < testCount ; i++){
            int fileIndex = random.nextInt(FILE_NAME_ARRAY.length);
            String fileName = FILE_NAME_ARRAY[fileIndex];

            this.node.getUdpQuery().searchFullFile(fileName);

            countSum += this.node.getCommunicationProvider().getQueryHopCount();
        }

        return (float) countSum/ (float) testCount;
    }
}
