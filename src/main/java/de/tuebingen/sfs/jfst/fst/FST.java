package de.tuebingen.sfs.jfst.fst;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;

/**
 * A finite-state transducer.
 */
public interface FST {

    /**
     * Write the FST to a file in JFST binary format.
     * @param out The output file
     * @throws IOException
     */
    void writeToBinary(OutputStream out) throws IOException;

    /**
     * @return The number of states in this transducer
     */
    int nOfStates();

    /**
     * @return The number of transitions in this transducer
     */
    int nOfTransitions();

    String[] getSymbols();

    /**
     * @return An iterator over the states of this transducer
     */
    FSTStateIterator iter();

    /**
     * Apply this transducer to an input string.
     * @param in The input string
     * @return The output strings matched to the input string by this transducer
     */
    Set<String> apply(String in);

    /**
     * Apply this transducer to an input string.
     * @param in The input string
     * @param ignoreInInput Also take transitions with these input symbols even if they do not occur in the input string
     * @return The output strings matched to the input string by this transducer
     */
    Set<String> apply(String in, Iterable<String> ignoreInInput);

    /**
     * Get all input strings in this transducer starting with prefix.
     * @param prefix Prefix of a string
     * @return The matching input strings in this transducer
     */
    Set<String> prefixSearch(String prefix);

    /**
     * Get all input strings in this transducer starting with prefix.
     * @param prefix Prefix of a string
     * @param maxSuffix The maximum number suffix transitions to take (to prevent infinite loop, default is 100)
     * @return The matching input strings in this transducer
     */
    Set<String> prefixSearch(String prefix, int maxSuffix);

    /**
     * Get all input strings in this transducer starting with prefix.
     * @param prefix Prefix of a string
     * @param ignoreInInput Also take transitions with these input symbols even if they do not occur in the input string
     * @return The matching input strings in this transducer
     */
    Set<String> prefixSearch(String prefix, Iterable<String> ignoreInInput);

    /**
     * Get all input strings in this transducer starting with prefix.
     * @param prefix Prefix of a string
     * @param maxSuffix The maximum number suffix transitions to take (to prevent infinite loop, default is 100)
     * @param ignoreInInput Also take transitions with these input symbols even if they do not occur in the input string
     * @return The matching input strings in this transducer
     */
    Set<String> prefixSearch(String prefix, int maxSuffix, Iterable<String> ignoreInInput);

}
