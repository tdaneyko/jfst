package de.tuebingen.sfs.jfst.fst;

import de.tuebingen.sfs.jfst.io.ATTWriter;
import de.tuebingen.sfs.jfst.io.BinaryFSTWriter;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;

/**
 * A finite-state transducer.
 */
public abstract class FST {

    private static final int MAX_SUFFIX = 100;

    /**
     * Write the FST to a file in JFST binary format.
     * @param out The output file
     * @throws IOException
     */
    public void writeToBinary(OutputStream out) throws IOException {
        BinaryFSTWriter.writeFST(out, this);
    }

    /**
     * Write the FST to a file in AT&T format.
     * @param attFile The output file
     * @throws IOException
     */
    public void writeToATT(File attFile) throws IOException {
        ATTWriter.writeFST(attFile, this);
    }

    /**
     * @return The number of states in this transducer
     */
    public abstract int nOfStates();

    /**
     * @return The number of transitions in this transducer
     */
    public abstract int nOfTransitions();

    public abstract String[] getSymbols();

    /**
     * @return An iterator over the states of this transducer
     */
    public abstract FSTStateIterator iter();

    /**
     * Apply this transducer to an input string.
     * @param in The input string
     * @return The output strings matched to the input string by this transducer
     */
    public Set<String> apply(String in) {
        return apply(in, null);
    }

    /**
     * Apply this transducer to an input string.
     * @param in The input string
     * @param ignoreInInput Also take transitions with these input symbols even if they do not occur in the input string
     * @return The output strings matched to the input string by this transducer
     */
    public abstract Set<String> apply(String in, Iterable<String> ignoreInInput);

    /**
     * Get all input strings in this transducer starting with prefix.
     * @param prefix Prefix of a string
     * @return The matching input strings in this transducer
     */
    public Set<String> prefixSearch(String prefix) {
        return prefixSearch(prefix, MAX_SUFFIX, null);
    }

    /**
     * Get all input strings in this transducer starting with prefix.
     * @param prefix Prefix of a string
     * @param maxSuffix The maximum number suffix transitions to take (to prevent infinite loop, default is 100)
     * @return The matching input strings in this transducer
     */
    public Set<String> prefixSearch(String prefix, int maxSuffix) {
        return prefixSearch(prefix, maxSuffix, null);
    }

    /**
     * Get all input strings in this transducer starting with prefix.
     * @param prefix Prefix of a string
     * @param ignoreInInput Also take transitions with these input symbols even if they do not occur in the input string
     * @return The matching input strings in this transducer
     */
    public Set<String> prefixSearch(String prefix, Iterable<String> ignoreInInput) {
        return prefixSearch(prefix, MAX_SUFFIX, ignoreInInput);
    }

    /**
     * Get all input strings in this transducer starting with prefix.
     * @param prefix Prefix of a string
     * @param maxSuffix The maximum number suffix transitions to take (to prevent infinite loop, default is 100)
     * @param ignoreInInput Also take transitions with these input symbols even if they do not occur in the input string
     * @return The matching input strings in this transducer
     */
    public abstract Set<String> prefixSearch(String prefix, int maxSuffix, Iterable<String> ignoreInInput);

}
