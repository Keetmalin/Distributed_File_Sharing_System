/* 
 * <Paste your header here>
 */
package org.uom.cse.distributed.peer.utils;

import java.util.HashSet;
import java.util.Set;

import static org.uom.cse.distributed.Constants.ADDRESSES_PER_CHARACTER;
import static org.uom.cse.distributed.Constants.CHARACTER_SPACE_SIZE;

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
        int index;
        if (Character.isLetter(c)) {
            index = (int) c - (int) 'A' + 10 + 1;
        } else if (Character.isDigit(c)) {
            index = (int) c - (int) '0' + 1;
        } else {
            throw new UnsupportedOperationException(c + " is not a letter or a digit");
        }

        index = index * ADDRESSES_PER_CHARACTER;

        return index;
    }

    public static char nodeIdToChar(int nodeId) {
        return nodeIdToChar(nodeId, false);
    }

    private static char nodeIdToChar(int nodeId, boolean reduced) {
        if (!reduced) {
            nodeId = nodeId / ADDRESSES_PER_CHARACTER - 1;
        }

        if (nodeId < 10) {
            return (char) (nodeId + (int) '0');
        } else {
            return (char) (nodeId + (int) 'A' - 10);
        }
    }

    /**
     * Finds the characters that should be undertaken by the node given by <code>nodeId</code>. <code>predecessor</code>
     * is the predecessor available for that node.
     * <p>
     * WARNING!!! - This method ommits the current node. Therefore, should do calculations for the current node
     * separately
     *
     * @param nodeId      ID of the node
     * @param predecessor predecessor of that node
     * @return list of characters belonging to the node.
     */
    public static Set<Character> findCharactersOf(int nodeId, int predecessor) {
        predecessor = predecessor / ADDRESSES_PER_CHARACTER - 1;
        predecessor = predecessor < CHARACTER_SPACE_SIZE - 1 ? predecessor + 1 : 0;
        nodeId = nodeId / ADDRESSES_PER_CHARACTER - 1;

        Set<Character> characters = new HashSet<>();

        while (predecessor != nodeId) {
            // Then all characters
            characters.add(nodeIdToChar(predecessor, true));
            predecessor++;
            if (predecessor == CHARACTER_SPACE_SIZE) {
                predecessor = 0;
            }
        }

        return characters;
    }
}
