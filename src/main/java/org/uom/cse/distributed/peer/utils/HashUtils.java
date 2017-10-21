/* 
 * <Paste your header here>
 */
package org.uom.cse.distributed.peer.utils;

import static org.uom.cse.distributed.Constants.ADDRESSES_PER_CHARACTER;

public class HashUtils {

    private HashUtils() { }

    /**
     * Converts the keyword into an integer which can later be used to determine the node in which this file's
     * information is stored.
     *
     * @param keyword keyword o be hashed
     * @return matching node ID
     */
    public static int keywordToNodeId(String keyword) {
        char c = Character.toUpperCase(keyword.charAt(0));
        int index = -1;
        if (Character.isLetter(c)) {
            index = Character.getNumericValue(c) - Character.getNumericValue('A') + 1;
        } else if (Character.isDigit(c)) {
            index = Character.getNumericValue(c) - Character.getNumericValue('0') + 1;
        } else {
            throw new UnsupportedOperationException(c + " is not a letter or a digit");
        }

        index = index * ADDRESSES_PER_CHARACTER;

        return index;
    }
}
