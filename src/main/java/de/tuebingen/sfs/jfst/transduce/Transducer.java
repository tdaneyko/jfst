package de.tuebingen.sfs.jfst.transduce;

import de.tuebingen.sfs.jfst.symbol.Alphabet;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;

/**
 * A finite-state transducer.
 */
public interface Transducer extends Automaton {

    /**
     * @return A mutable version of this transducer
     */
    MutableTransducer getMutableCopy();

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
