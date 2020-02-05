package de.tuebingen.sfs.jfst.fst;

import de.tuebingen.sfs.jfst.alphabet.Alphabet;

import java.io.*;
import java.util.*;

/**
 * A mutable FST to which new states and transitions may be added.
 */
public class CompactMutableFST extends FST {

    // Literal symbols used by the transliterator
    private Alphabet alphabet;
    // Id of the identity (copy) symbol
//    private final int idIdx;

    /*
    The transitions of the transliterator. In each long, the first 16 bit encode the input symbol,
    the next 16 bit the output symbol and the final 32 bit the id of the to-state. For each state,
    the transitions are ordered.
    */
    private List<List<Transition>> transitions;
    // Whether a state with id index is accepting or not.
    private List<Boolean> accepting;

    // The start state
    private int start = 0;
    // Id of next state
    private int s = 1;

    @Override
    public void writeToBinary(OutputStream out) throws IOException {

    }

    @Override
    public int nOfStates() {
        return transitions.size();
    }

    @Override
    public int nOfTransitions() {
        return transitions.stream().mapToInt(List::size).sum();
    }

    @Override
    public String[] getSymbols() {
        return new String[0];
    }

    @Override
    public FSTStateIterator iter() {
        return null;
    }

    @Override
    public Set<String> apply(String in, Iterable<String> ignoreInInput) {
        return null;
    }

    @Override
    public Set<String> prefixSearch(String prefix, int maxSuffix, Iterable<String> ignoreInInput) {
        return null;
    }
}
