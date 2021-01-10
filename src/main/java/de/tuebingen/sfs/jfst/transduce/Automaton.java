package de.tuebingen.sfs.jfst.transduce;

import de.tuebingen.sfs.jfst.symbol.Alphabet;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

public interface Automaton {

    /**
     * Write the Automaton to a file in JFST binary format.
     * @param out The output file
     * @throws IOException
     */
    void writeToBinary(OutputStream out) throws IOException;

    /**
     * Write the Automaton to a file in AT&T format.
     * @param attFile The output file
     * @throws IOException
     */
    void writeToATT(File attFile) throws IOException;

    /**
     * @return The number of states in this automaton
     */
    int nOfStates();

    /**
     * @return The number of transitions in this automaton
     */
    int nOfTransitions();

    /**
     * @return The alphabet of this automaton
     */
    Alphabet getAlphabet();

    /**
     * @return An iterator over the states of this automaton
     */
    StateIterator iter();

}
