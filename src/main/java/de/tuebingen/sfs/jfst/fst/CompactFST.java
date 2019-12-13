package de.tuebingen.sfs.jfst.fst;

import de.tuebingen.sfs.jfst.alphabet.Alphabet;
import de.tuebingen.sfs.jfst.alphabet.Symbol;
import de.tuebingen.sfs.jfst.io.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

/**
 * A compact, memory-efficient FST.
 */
public class CompactFST implements FST {

    private static final int MAX_SUFFIX = 100;
    private static final int MAX_INSERTIONS = 15;

    // Mask to get the input symbol out of a transition
    private static final long getInSym = 0xffff000000000000L;
    // Mask to get the output symbol out of a transition
    private static final long getOutSym = 0x0000ffff00000000L;
    // Mask to get the to-state out of a transition
    private static final long getToState = 0x00000000ffffffffL;

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
    private int start = 0;
    // The starting index of a state's transitions in the transitions list (index = state id).
    private int[] stateOffsets;
    // Whether a state with id index is accepting or not.
    private boolean[] accepting;

    /**
     * Create a compact FST from a set of states with transitions and an alphabet.
     * @param iter An iterator over states and transitions
     */
    public CompactFST(FSTStateIterator iter) {
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
     * Calls MutableFST.readFromATT(in, producer).makeCompact() internally.
     * @param in AT&amp;T file
     * @param producer Original producer of the file
     * @return The FST specified by the file
     */
    public static CompactFST readFromATT(InputStream in, FSTProducer producer) {
        return readFromATT(in, producer, false);
    }

    /**
     * Parse a file in AT&amp;T format into a CompactFST.
     * Calls MutableFST.readFromATT(in, producer, reverse).makeCompact() internally.
     * @param in AT&amp;T file
     * @param producer Original producer of the file
     * @param reverse False: Input symbol comes before output symbol; True: Output symbol comes before input symbol
     * @return The FST specified by the file
     */
    public static CompactFST readFromATT(InputStream in, FSTProducer producer, boolean reverse) {
        return MutableFST.readFromATT(in, producer, reverse).makeCompact();
    }

    /**
     * Load a Compact FST from a binary JFST file.
     * @param fileName The path to the JFST file
     * @return The FST specified by the file
     */
    public static CompactFST readFromBinary(String fileName) {
        return readFromBinary(fileName, false);
    }

    /**
     * Load a Compact FST from a binary FST file.
     * @param fileName The path to the FST file
     * @param producer Original producer of the file
     * @return The FST specified by the file
     */
    public static CompactFST readFromBinary(String fileName, FSTProducer producer) {
        return readFromBinary(fileName, producer, false);
    }

    /**
     * Load a Compact FST from a binary JFST file.
     * @param fileName The path to the JFST file
     * @param inverse If true, invert input and output symbols
     * @return The FST specified by the file
     */
    public static CompactFST readFromBinary(String fileName, boolean inverse) {
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
    public static CompactFST readFromBinary(String fileName, FSTProducer producer, boolean inverse) {
        if (producer.equals(FSTProducer.SFST)) {
            System.err.println("Cannot read SFST binary files (yet).");
            return null;
        }
        FSTFileStateIterator iter = (producer.equals(FSTProducer.JFST))
                ? new JFSTFileStateIterator(fileName, inverse)
                : new HFSTFileStateIterator(fileName, inverse);
        CompactFST fst = new CompactFST(iter);
        iter.close();
        return fst;
    }

    @Override
    public void writeToBinary(OutputStream out) throws IOException {
        BinaryFSTWriter.writeFST(out, this);
    }

    private long makeTransition(long inSym, long outSym, long toId) {
        return (((inSym << 16) | outSym) << 32) | toId;
    }

    private int inIdFromTransition(long transition) {
        return (int) ((transition & getInSym) >> 48);
    }

    private int outIdFromTransition(long transition) {
        return (int) ((transition & getOutSym) >> 32);
    }

    private int toIdFromTransition(long transition) {
        return (int) (transition & getToState);
    }

    private boolean isIdentityTransition(long transition) {
        return inIdFromTransition(transition) == idIdx;
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
    public String[] getSymbols() {
        return alphabet.getSymbols();
    }

    @Override
    public CompactFSTStateIterator iter() {
        return new CompactFSTStateIterator(this);
    }
    public String depth;
    @Override
    public Set<String> apply(String in) {
        return apply(in, null);
    }

    public Set<String> apply(String in, int maxInsertions) {
        return apply(in, maxInsertions, null);
    }

    @Override
    public Set<String> apply(String in, Iterable<String> ignoreInInput) {
        return apply(in, MAX_INSERTIONS, ignoreInInput);
    }

    public Set<String> apply(String in, int maxInsertions, Iterable<String> ignoreInInput) {
        depth = "";
        return apply(in, 0, start, 0, maxInsertions, ignoreInInput);
    }

    private Set<String> apply(String s, int strIdx, int statIdx, int ins, int maxIns, Iterable<String> ignoreInInput) {
        depth += "\t";
        // String has been consumed?
        boolean sFin = strIdx >= s.length();

        // Result set
        Set<String> res = new HashSet<>();

        // Return empty string if accepting
        if (sFin && accepting[statIdx])
            res.add("");

        int x = res.size();

        // Apply ignore transitions
        if (ignoreInInput != null) {
            for (String ign : ignoreInInput) {
                TransitionIterator ignIter = new TransitionIterator(ign, statIdx);
                while (ignIter.hasNext()) {
                    Transition trans = ignIter.next();
                    Set<String> prev = apply(s, strIdx, trans.toState, ins, maxIns, ignoreInInput);
                    if (isEpsilon(trans.outSym))
                        res.addAll(prev);
                    else {
                        for (String r : prev)
                            res.add(trans.outSym + r);
                    }
                }
            }
        }

        // Apply epsilon transitions
        // Apply at most maxIns epsilons
        if (ins < maxIns) {
            TransitionIterator epsIter = new TransitionIterator(Symbol.EPSILON_STRING, statIdx);
            while (epsIter.hasNext()) {
                Transition trans = epsIter.next();
                Set<String> prev = apply(s, strIdx, trans.toState, ins + 1, maxIns, ignoreInInput);
                if (isEpsilon(trans.outSym))
                    res.addAll(prev);
                else {
                    for (String r : prev)
                        res.add(trans.outSym + r);
                }
            }
        }

        // If there is a char left in the string...
        if (!sFin) {
            // ...apply matching literal transitions
            for (Symbol pref : alphabet.getPrefixes(s, strIdx)) {
                TransitionIterator litIter = new TransitionIterator(pref.asString(), statIdx);
                while (litIter.hasNext()) {
                    Transition trans = litIter.next();
                    Set<String> prev = apply(s, strIdx + pref.length(), trans.toState, 0, maxIns, ignoreInInput);
                    if (isEpsilon(trans.outSym))
                        res.addAll(prev);
                    else {
                        for (String r : prev)
                            res.add(trans.outSym + r);
                    }
                }
            }

            // ...and identity transitions
            char c = s.charAt(strIdx);
            if (!alphabet.contains(c)) {
                TransitionIterator idIter = new TransitionIterator(Symbol.IDENTITY_STRING, statIdx);
                while (idIter.hasNext()) {
                    Transition trans = idIter.next();
                    Set<String> prev = apply(s, strIdx + 1, trans.toState, 0, maxIns, ignoreInInput);
                    for (String r : prev)
                        res.add(c + r);
                    if (!prev.isEmpty())
                        break;
                }
            }
        }

        return res;
    }

    @Override
    public Set<String> prefixSearch(String prefix) {
        return prefixSearch(prefix, MAX_SUFFIX, null);
    }

    @Override
    public Set<String> prefixSearch(String prefix, int maxSuffix) {
        return prefixSearch(prefix, maxSuffix, null);
    }

    @Override
    public Set<String> prefixSearch(String prefix, Iterable<String> ignoreInInput) {
        return prefixSearch(prefix, MAX_SUFFIX, ignoreInInput);
    }

    @Override
    public Set<String> prefixSearch(String prefix, int maxSuffix, Iterable<String> ignoreInInput) {
        return prefixSearch(prefix, 0, 0, maxSuffix, ignoreInInput);
    }

    private Set<String> prefixSearch(String s, int strIdx, int statIdx, int maxSuffix, Iterable<String> ignoreInInput) {
        // String has been consumed?
        boolean sFin = strIdx >= s.length();

        if (sFin)
            maxSuffix--;

        // Result set
        Set<String> res = new HashSet<>();

        // Return empty string if accepting
        if (sFin && accepting[statIdx])
            res.add("");

        if (!sFin) {
            // Apply ignore transitions
            if (ignoreInInput != null) {
                for (String ign : ignoreInInput) {
                    TransitionIterator ignIter = new TransitionIterator(ign, statIdx);
                    while (ignIter.hasNext()) {
                        Transition trans = ignIter.next();
                        Set<String> prev = prefixSearch(s, strIdx, trans.toState, maxSuffix, ignoreInInput);
                            for (String r : prev)
                                res.add(ign + r);
                    }
                }
            }

            // Apply epsilon transitions
            TransitionIterator epsIter = new TransitionIterator(Symbol.EPSILON_STRING, statIdx);
            while (epsIter.hasNext()) {
                Transition trans = epsIter.next();
                Set<String> prev = prefixSearch(s, strIdx, trans.toState, maxSuffix, ignoreInInput);
                    res.addAll(prev);
            }

            // If there is a char left in the string...
            // ...apply matching literal transitions
            for (Symbol pref : alphabet.getPrefixes(s, strIdx)) {
                TransitionIterator litIter = new TransitionIterator(pref.asString(), statIdx);
                while (litIter.hasNext()) {
                    Transition trans = litIter.next();
                    Set<String> prev = prefixSearch(s, strIdx + pref.length(), trans.toState, maxSuffix, ignoreInInput);
                    for (String r : prev)
                        res.add(pref + r);
                }
            }

            // ...and identity transitions
            char c = s.charAt(strIdx);
            if (!alphabet.contains(c)) {
                TransitionIterator idIter = new TransitionIterator(Symbol.IDENTITY_STRING, statIdx);
                while (idIter.hasNext()) {
                    Transition trans = idIter.next();
                    Set<String> prev = prefixSearch(s, strIdx + 1, trans.toState, maxSuffix, ignoreInInput);
                    for (String r : prev)
                        res.add(c + r);
                }
            }
        }
        else if (maxSuffix >= 0) {
            TransitionIterator allIter = new TransitionIterator(statIdx);
            while (allIter.hasNext()) {
                Transition trans = allIter.next();
                String inSym = trans.inSym;
                Set<String> prev = prefixSearch(s, strIdx + inSym.length(), trans.toState, maxSuffix, ignoreInInput);
                for (String r : prev)
                    res.add(inSym + r);
            }
        }

        return res;
    }

    private boolean isEpsilon(String s) {
        return s != null && s.length() == 1 && s.charAt(0) == Symbol.EPSILON_CHAR;
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

    // Helper class to store a to-state and an output symbol
    private class Transition {

        final int toState;
        final String inSym;
        final String outSym;

        public Transition(int toState, String inSym, String outSym) {
            this.toState = toState;
            this.inSym = inSym;
            this.outSym = outSym;
        }

        @Override
        public String toString() {
            return inSym + ":" + outSym + " => " + toState;
        }
    }


    private static class CompactFSTStateIterator implements FSTStateIterator {

        final CompactFST fst;
        final Alphabet alphabet;

        int s;
        int t;
        int tend;

        public CompactFSTStateIterator(CompactFST fst) {
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
            t = fst.stateOffsets[s];
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
            return fst.isIdentityTransition(fst.transitions[t]);
        }

        @Override
        public int inId() {
            if (identity())
                return -1;
            else
                return fst.inIdFromTransition(fst.transitions[t]);
        }

        @Override
        public int outId() {
            if (identity())
                return -1;
            else
                return fst.outIdFromTransition(fst.transitions[t]);
        }

        @Override
        public int toId() {
            if (identity())
                return -1;
            else
                return fst.toIdFromTransition(fst.transitions[t]);
        }
    }
}
