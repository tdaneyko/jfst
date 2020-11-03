package de.tuebingen.sfs.jfst.transduce.compact;

import de.tuebingen.sfs.jfst.symbol.Alphabet;
import de.tuebingen.sfs.jfst.io.*;
import de.tuebingen.sfs.jfst.transduce.ApplicableTransducer;
import de.tuebingen.sfs.jfst.transduce.MutableTransducer;
import de.tuebingen.sfs.jfst.transduce.StateIterator;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Iterator;

import static de.tuebingen.sfs.jfst.transduce.compact.CompactTransition.*;

/**
 * A compact, memory-efficient Transducer.
 */
public class CompactTransducer extends ApplicableTransducer {

    // Literal symbols used by the transliterator
    private Alphabet alphabet;

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
     * Create a compact Transducer from a set of states with transitions and an alphabet.
     * @param iter An iterator over states and transitions
     */
    public CompactTransducer(StateIterator iter) {
        // Set start state
        this.start = iter.getStartState();
        // Copy alphabet
        this.alphabet = iter.getAlphabet();

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
                transitions[t] = makeTransition(iter.inId(), iter.outId(), iter.toId());
                t++;
            }
            // Sort transitions for current state
            Arrays.sort(transitions, stateOffsets[s], t);
            s++;
        }
    }

    /**
     * Parse a file in AT&amp;T format into a CompactTransducer.
     * Calls MutableFSTOld.readFromATT(in, producer).makeCompact() internally.
     * @param in AT&amp;T file
     * @param producer Original producer of the file
     * @return The Transducer specified by the file
     */
    public static CompactTransducer readFromATT(InputStream in, FstProducer producer) {
        return readFromATT(in, producer, false);
    }

    /**
     * Parse a file in AT&amp;T format into a CompactTransducer.
     * Calls MutableFSTOld.readFromATT(in, producer, reverse).makeCompact() internally.
     * @param in AT&amp;T file
     * @param producer Original producer of the file
     * @param reverse False: Input symbol comes before output symbol; True: Output symbol comes before input symbol
     * @return The Transducer specified by the file
     */
    public static CompactTransducer readFromATT(InputStream in, FstProducer producer, boolean reverse) {
//        return MutableFSTOld.readFromATT(in, producer, reverse).makeCompact();
        return new CompactTransducer(new AttFileStateIterator(in, producer, reverse));
    }

    /**
     * Load a Compact Transducer from a binary JFST file.
     * @param fileName The path to the JFST file
     * @return The Transducer specified by the file
     */
    public static CompactTransducer readFromBinary(String fileName) {
        return readFromBinary(fileName, false);
    }

    /**
     * Load a Compact Transducer from a binary Transducer file.
     * @param fileName The path to the Transducer file
     * @param producer Original producer of the file
     * @return The Transducer specified by the file
     */
    public static CompactTransducer readFromBinary(String fileName, FstProducer producer) {
        return readFromBinary(fileName, producer, false);
    }

    /**
     * Load a Compact Transducer from a binary JFST file.
     * @param fileName The path to the JFST file
     * @param inverse If true, invert input and output symbols
     * @return The Transducer specified by the file
     */
    public static CompactTransducer readFromBinary(String fileName, boolean inverse) {
        FstProducer producer = (fileName.endsWith(".hfst")) ? FstProducer.HFST_INTERNAL : FstProducer.JFST;
        return readFromBinary(fileName, producer, inverse);
    }

    /**
     * Load a Compact Transducer from a binary Transducer file.
     * @param fileName The path to the Transducer file
     * @param producer Original producer of the file
     * @param inverse If true, invert input and output symbols
     * @return The Transducer specified by the file
     */
    public static CompactTransducer readFromBinary(String fileName, FstProducer producer, boolean inverse) {
        if (producer.equals(FstProducer.SFST)) {
            System.err.println("Cannot read SFST binary files (yet).");
            return null;
        }
        FileStateIterator iter = (producer.equals(FstProducer.JFST))
                ? new JfstFileStateIterator(fileName, inverse)
                : new HfstFileStateIterator(fileName, inverse);
        CompactTransducer fst = new CompactTransducer(iter);
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
    public CompactTransducerStateIterator iter() {
        return new CompactTransducerStateIterator(this);
    }

    @Override
    public MutableTransducer getMutableCopy() {
        return new MutableCompactTransducer(iter());
    }

    @Override
    public int getStartState() {
        return start;
    }

    @Override
    public boolean isAccepting(int stateId) {
        return accepting[stateId];
    }

    @Override
    public Iterator<Transition> getTransitionIterator(int statIdx) {
        return new TransitionIterator(statIdx);
    }

    @Override
    public Iterator<Transition> getTransitionIterator(String s, int statIdx) {
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
                long ci = alphabet.getId(s);
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
            return i < end && (inSym == 1 || (transitions[i] & GET_IN_SYM) == inSym);
        }

        @Override
        public Transition next() {
            int toState = toIdFromTransition(transitions[i]);
            int outIdx = outIdFromTransition(transitions[i]);
            String out = (outIdx == alphabet.identityId()) ? inC : alphabet.getSymbol(outIdx).toString();
            String in = (inSym == 1) ? alphabet.getSymbol(inIdFromTransition(transitions[i])).toString() : inC;
            i++;
            return new Transition(toState, in, out);
        }
    }


    private static class CompactTransducerStateIterator implements StateIterator {

        final CompactTransducer fst;
        final Alphabet alphabet;

        int s;
        int t;
        int tend;

        public CompactTransducerStateIterator(CompactTransducer fst) {
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
        public int inId() {
            return inIdFromTransition(fst.transitions[t]);
        }

        @Override
        public int outId() {
            return outIdFromTransition(fst.transitions[t]);
        }

        @Override
        public int toId() {
            return toIdFromTransition(fst.transitions[t]);
        }
    }
}
