package de.tuebingen.sfs.jfst.fst;

import de.tuebingen.sfs.jfst.alphabet.Alphabet;
import de.tuebingen.sfs.jfst.alphabet.Symbol;
import de.tuebingen.sfs.jfst.io.*;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Iterator;

import static de.tuebingen.sfs.jfst.fst.CompactTransition.*;

/**
 * A compact, memory-efficient FST.
 */
public class CompactFST2 extends ApplicableFST {

    // Literal symbols used by the transliterator
    private Alphabet alphabet;
    // Id of the identity (copy) symbol
    private final int idIdx;

    /*
    The transitions of the transliterator. In each long, the first 16 bit encode the input symbol,
    the next 16 bit the output symbol and the final 32 bit the id of the to-state. For each state,
    the transitions are ordered.
     */
    private long[] transitions;

    // The start state
    private int start;
    // The starting index of a state's transitions in the transitions list (index = state id).
    private int[] stateOffsets;
    // Whether a state with id index is accepting or not.
    private boolean[] accepting;

    /**
     * Create a compact FST from a set of states with transitions and an alphabet.
     * @param iter An iterator over states and transitions
     */
    public CompactFST2(FSTStateIterator iter) {
        // Set start state
        this.start = iter.getStartState();
        // Copy alphabet
        this.alphabet = iter.getAlphabet();
        // Add identity symbol
        this.idIdx = iter.getIdentityId();
        if (idIdx == alphabet.size())
            this.alphabet.addSymbol(Symbol.IDENTITY_STRING);

        // Initialize state and transition lists
        this.stateOffsets = new int[iter.nOfStates()];
        this.accepting = new boolean[iter.nOfStates()];
        this.transitions = new long[iter.nOfTransitions()];
        int s = 0; // Id of current state
        int t = 0; // Index of state's current transition
        // Store states and transitions
        while (iter.hasNextState()) {
            iter.nextState();
            stateOffsets[s] = t;
            accepting[s] = iter.accepting();
            while (iter.hasNextTransition()) {
                iter.nextTransition();
                if (iter.identity())
                    transitions[t] = makeTransition(idIdx, idIdx, iter.toId());
                else
                    transitions[t] = makeTransition(iter.inId(), iter.outId(), iter.toId());
                t++;
            }
            // Sort transitions for current state
            Arrays.sort(transitions, stateOffsets[s], t);
            s++;
        }
    }

    /**
     * Parse a file in AT&amp;T format into a CompactFST.
     * Calls MutableFSTOld.readFromATT(in, producer).makeCompact() internally.
     * @param in AT&amp;T file
     * @param producer Original producer of the file
     * @return The FST specified by the file
     */
    public static CompactFST2 readFromATT(InputStream in, FSTProducer producer) {
        return readFromATT(in, producer, false);
    }

    /**
     * Parse a file in AT&amp;T format into a CompactFST.
     * Calls MutableFSTOld.readFromATT(in, producer, reverse).makeCompact() internally.
     * @param in AT&amp;T file
     * @param producer Original producer of the file
     * @param reverse False: Input symbol comes before output symbol; True: Output symbol comes before input symbol
     * @return The FST specified by the file
     */
    public static CompactFST2 readFromATT(InputStream in, FSTProducer producer, boolean reverse) {
//        return MutableFSTOld.readFromATT(in, producer, reverse).makeCompact();
        return new CompactFST2(new ATTFileStateIterator(in, producer, reverse));
    }

    /**
     * Load a Compact FST from a binary JFST file.
     * @param fileName The path to the JFST file
     * @return The FST specified by the file
     */
    public static CompactFST2 readFromBinary(String fileName) {
        return readFromBinary(fileName, false);
    }

    /**
     * Load a Compact FST from a binary FST file.
     * @param fileName The path to the FST file
     * @param producer Original producer of the file
     * @return The FST specified by the file
     */
    public static CompactFST2 readFromBinary(String fileName, FSTProducer producer) {
        return readFromBinary(fileName, producer, false);
    }

    /**
     * Load a Compact FST from a binary JFST file.
     * @param fileName The path to the JFST file
     * @param inverse If true, invert input and output symbols
     * @return The FST specified by the file
     */
    public static CompactFST2 readFromBinary(String fileName, boolean inverse) {
        FSTProducer producer = (fileName.endsWith(".hfst")) ? FSTProducer.HFST : FSTProducer.JFST;
        return readFromBinary(fileName, producer, inverse);
    }

    /**
     * Load a Compact FST from a binary FST file.
     * @param fileName The path to the FST file
     * @param producer Original producer of the file
     * @param inverse If true, invert input and output symbols
     * @return The FST specified by the file
     */
    public static CompactFST2 readFromBinary(String fileName, FSTProducer producer, boolean inverse) {
        if (producer.equals(FSTProducer.SFST)) {
            System.err.println("Cannot read SFST binary files (yet).");
            return null;
        }
        FSTFileStateIterator iter = (producer.equals(FSTProducer.JFST))
                ? new JFSTFileStateIterator(fileName, inverse)
                : new HFSTFileStateIterator(fileName, inverse);
        CompactFST2 fst = new CompactFST2(iter);
        iter.close();
        return fst;
    }

    @Override
    public int nOfStates() {
        return stateOffsets.length;
    }

    @Override
    public int nOfTransitions() {
        return transitions.length;
    }

    @Override
    public Alphabet getAlphabet() {
        return alphabet;
    }

    @Override
    public CompactFSTStateIterator iter() {
        return new CompactFSTStateIterator(this);
    }

    @Override
    int getStartState() {
        return start;
    }

    @Override
    boolean isAccepting(int stateId) {
        return accepting[stateId];
    }

    @Override
    Iterator<Transition> getTransitionIterator(int statIdx) {
        return new TransitionIterator(statIdx);
    }

    @Override
    Iterator<Transition> getTransitionIterator(String s, int statIdx) {
        return new TransitionIterator(s, statIdx);
    }

    // Helper class to iterate over transitions with a specific input symbol
    private class TransitionIterator implements Iterator<Transition> {

        private final String inC;
        private final long inSym;

        private int i;
        private final int end;

        public TransitionIterator(String s, int statIdx) {
            inC = s;
            if (alphabet.contains(s)) {
                long ci = alphabet.idOf(s);
                inSym = ci << 48;
                end = (statIdx == stateOffsets.length - 1) ? transitions.length : stateOffsets[statIdx + 1];
                i = Arrays.binarySearch(transitions, stateOffsets[statIdx], end, inSym);
                if (i < 0)
                    i = -(i + 1);
                else {
                    while (i > 0 && transitions[i - 1] == inSym)
                        i--;
                }
            }
            else {
                inSym = 0;
                i = Integer.MAX_VALUE;
                end = Integer.MIN_VALUE;
            }
        }

        public TransitionIterator(int statIdx) {
            inC = "";
            inSym = 1;
            i = stateOffsets[statIdx];
            end = (statIdx == stateOffsets.length - 1) ? transitions.length : stateOffsets[statIdx + 1];
        }

        @Override
        public boolean hasNext() {
            return i < end && (inSym == 1 || (transitions[i] & getInSym) == inSym);
        }

        @Override
        public Transition next() {
            int toState = toIdFromTransition(transitions[i]);
            int outIdx = outIdFromTransition(transitions[i]);
            String out = (outIdx == idIdx) ? inC : alphabet.getSymbol(outIdx).toString();
            String in = (inSym == 1) ? alphabet.getSymbol(inIdFromTransition(transitions[i])).toString() : inC;
            i++;
            return new Transition(toState, in, out);
        }
    }


    private static class CompactFSTStateIterator implements FSTStateIterator {

        final CompactFST2 fst;
        final Alphabet alphabet;

        int s;
        int t;
        int tend;

        public CompactFSTStateIterator(CompactFST2 fst) {
            this.fst = fst;
            this.alphabet = new Alphabet(fst.alphabet.getSymbols());

            s = -1;
            t = -1;
            tend = -1;
        }

        @Override
        public int nOfStates() {
            return fst.nOfStates();
        }

        @Override
        public int nOfTransitions() {
            return fst.nOfTransitions();
        }

        @Override
        public Alphabet getAlphabet() {
            return alphabet;
        }

        @Override
        public int getStartState() {
            return fst.start;
        }

        @Override
        public int getIdentityId() {
            return fst.idIdx;
        }

        @Override
        public boolean hasNextState() {
            return s+1 < fst.stateOffsets.length;
        }

        @Override
        public void nextState() {
            s++;
            t = fst.stateOffsets[s] - 1;
            tend = (s == fst.stateOffsets.length-1) ? fst.transitions.length : fst.stateOffsets[s+1];
        }

        @Override
        public boolean accepting() {
            return fst.accepting[s];
        }

        @Override
        public boolean hasNextTransition() {
            return t+1 < tend;
        }

        @Override
        public void nextTransition() {
            t++;
        }

        @Override
        public boolean identity() {
            return isIdentityTransition(fst.transitions[t], fst.idIdx);
        }

        @Override
        public int inId() {
            if (identity())
                return -1;
            else
                return inIdFromTransition(fst.transitions[t]);
        }

        @Override
        public int outId() {
            if (identity())
                return -1;
            else
                return outIdFromTransition(fst.transitions[t]);
        }

        @Override
        public int toId() {
            return toIdFromTransition(fst.transitions[t]);
        }
    }
}
