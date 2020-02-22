package de.tuebingen.sfs.jfst.fst;

import de.tuebingen.sfs.jfst.alphabet.Alphabet;
import de.tuebingen.sfs.jfst.io.ATTWriter;
import de.tuebingen.sfs.jfst.io.BinaryFSTWriter;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;

/**
 * A finite-state transducer.
 */
public interface FST2 {

    /**
     * Write the FST to a file in JFST binary format.
     * @param out The output file
     * @throws IOException
     */
    void writeToBinary(OutputStream out) throws IOException;

    /**
     * Write the FST to a file in AT&T format.
     * @param attFile The output file
     * @throws IOException
     */
    void writeToATT(File attFile) throws IOException;

    /**
     * @return The number of states in this transducer
     */
    int nOfStates();

    /**
     * @return The number of transitions in this transducer
     */
    int nOfTransitions();

    /**
     * @return The alphabet of this transducer
     */
    Alphabet getAlphabet();

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
     * Get all input strings in this transducer starting with prefix.
     * @param prefix Prefix of a string
     * @return The matching input strings in this transducer
     */
    Set<String> prefixSearch(String prefix);

}
