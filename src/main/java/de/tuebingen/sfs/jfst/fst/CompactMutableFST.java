package de.tuebingen.sfs.jfst.fst;

import de.tuebingen.sfs.jfst.alphabet.Alphabet;
import de.tuebingen.sfs.jfst.alphabet.Symbol;
import de.tuebingen.sfs.jfst.io.*;

import java.io.InputStream;
import java.util.*;

/**
 * A mutable FST to which new states and transitions may be added.
 */
public class CompactMutableFST extends ApplicableFST {

    // Literal symbols used by the transliterator
    private Alphabet alphabet;
    // Id of the identity (copy) symbol
    private final int idIdx;

    /*
    The transitions of the transliterator. In each long, the first 16 bit encode the input symbol,
    the next 16 bit the output symbol and the final 32 bit the id of the to-state. For each state,
    the transitions are ordered.
    */
    private List<List<CompactTransition>> transitions;
    // Whether a state with id index is accepting or not.
    private List<Boolean> accepting;

    // The start state
    private int start;
    // Id of next state
    private int s = 0;

    public CompactMutableFST() {
        this.alphabet = new Alphabet();
        this.idIdx = alphabet.getIdOrCreate(Symbol.IDENTITY_STRING);
        this.transitions = new ArrayList<>();
        this.accepting = new ArrayList<>();
        this.start = addState(false);
    }

    public CompactMutableFST(FSTStateIterator iter) {
        // Set start state
        this.start = iter.getStartState();
        // Copy alphabet
        this.alphabet = iter.getAlphabet();
        // Add identity symbol
        this.idIdx = iter.getIdentityId();
        if (idIdx == alphabet.size())
            this.alphabet.addSymbol(Symbol.IDENTITY_STRING);

        // Initialize state and transition lists
        this.transitions = new ArrayList<>();
        this.accepting = new ArrayList<>();
        // Store states and transitions
        while (iter.hasNextState()) {
            iter.nextState();
            List<CompactTransition> stateTrans = new ArrayList<>();
            accepting.add(iter.accepting());
            while (iter.hasNextTransition()) {
                iter.nextTransition();
                if (iter.identity())
                    stateTrans.add(new CompactTransition(idIdx, idIdx, iter.toId()));
                else
                    stateTrans.add(new CompactTransition(iter.inId(), iter.outId(), iter.toId()));
            }
            // Sort transitions for current state
            Collections.sort(stateTrans);
            transitions.add(stateTrans);
        }
    }

    private int addState(boolean accepting) {
        this.transitions.add(new ArrayList<>());
        this.accepting.add(accepting);
        return s++;
    }

    /**
     * Parse a file in AT&amp;T format into a CompactFST.
     * Calls MutableFSTOld.readFromATT(in, producer).makeCompact() internally.
     * @param in AT&amp;T file
     * @param producer Original producer of the file
     * @return The FST specified by the file
     */
    public static CompactMutableFST readFromATT(InputStream in, FSTProducer producer) {
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
    public static CompactMutableFST readFromATT(InputStream in, FSTProducer producer, boolean reverse) {
//        return MutableFSTOld.readFromATT(in, producer, reverse).makeCompact();
        return new CompactMutableFST(new ATTFileStateIterator(in, producer, reverse));
    }

    /**
     * Load a Compact FST from a binary JFST file.
     * @param fileName The path to the JFST file
     * @return The FST specified by the file
     */
    public static CompactMutableFST readFromBinary(String fileName) {
        return readFromBinary(fileName, false);
    }

    /**
     * Load a Compact FST from a binary FST file.
     * @param fileName The path to the FST file
     * @param producer Original producer of the file
     * @return The FST specified by the file
     */
    public static CompactMutableFST readFromBinary(String fileName, FSTProducer producer) {
        return readFromBinary(fileName, producer, false);
    }

    /**
     * Load a Compact FST from a binary JFST file.
     * @param fileName The path to the JFST file
     * @param inverse If true, invert input and output symbols
     * @return The FST specified by the file
     */
    public static CompactMutableFST readFromBinary(String fileName, boolean inverse) {
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
    public static CompactMutableFST readFromBinary(String fileName, FSTProducer producer, boolean inverse) {
        if (producer.equals(FSTProducer.SFST)) {
            System.err.println("Cannot read SFST binary files (yet).");
            return null;
        }
        FSTFileStateIterator iter = (producer.equals(FSTProducer.JFST))
                ? new JFSTFileStateIterator(fileName, inverse)
                : new HFSTFileStateIterator(fileName, inverse);
        CompactMutableFST fst = new CompactMutableFST(iter);
        iter.close();
        return fst;
    }

    /**
     * @return The compact version of this FST
     */
    public CompactFST2 makeCompact() {
        return new CompactFST2(this.iter());
    }

    @Override
    int getStartState() {
        return start;
    }

    @Override
    boolean isAccepting(int stateId) {
        return accepting.get(stateId);
    }

    @Override
    Iterator<Transition> getTransitionIterator(int statIdx) {
        return new TransitionIterator(statIdx);
    }

    @Override
    Iterator<Transition> getTransitionIterator(String s, int statIdx) {
        return new TransitionIterator(s, statIdx);
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
    public Alphabet getAlphabet() {
        return alphabet;
    }

    @Override
    public FSTStateIterator iter() {
        return new CompactFSTStateIterator(this);
    }

    private class TransitionIterator implements Iterator<Transition> {

        private final String inC;
        private final long inSym;

        private int i;
        private final List<CompactTransition> stateTrans;

        public TransitionIterator(String s, int statIdx) {
            inC = s;
            if (alphabet.contains(s)) {
                long ci = alphabet.idOf(s);
                inSym = ci << 48;
                stateTrans = transitions.get(statIdx);
                i = Collections.binarySearch(stateTrans, inSym);
                if (i < 0)
                    i = -(i + 1);
                else {
                    while (i > 0 && stateTrans.get(i - 1).getInternalRepresentation() == inSym)
                        i--;
                }
            }
            else {
                inSym = 0;
                i = Integer.MAX_VALUE;
                stateTrans = new ArrayList<>();
            }
        }

        public TransitionIterator(int statIdx) {
            inC = "";
            inSym = 1;
            i = 0;
            stateTrans = transitions.get(statIdx);
        }

        @Override
        public boolean hasNext() {
            return i < stateTrans.size() && (inSym == 1 ||
                    (stateTrans.get(i).getInternalRepresentation() & CompactTransition.getInSym) == inSym);
        }

        @Override
        public Transition next() {
            CompactTransition transition = stateTrans.get(i);
            int toState = transition.getToState();
            int outIdx = transition.getOutSym();
            String out = (outIdx == idIdx) ? inC : alphabet.getSymbol(outIdx).toString();
            String in = (inSym == 1) ? alphabet.getSymbol(transition.getInSym()).toString() : inC;
            i++;
            return new Transition(toState, in, out);
        }
    }

    private static class CompactFSTStateIterator implements FSTStateIterator {

        final CompactMutableFST fst;
        final Alphabet alphabet;

        int s;
        int t;

        List<CompactTransition> stateTrans;

        public CompactFSTStateIterator(CompactMutableFST fst) {
            this.fst = fst;
            this.alphabet = new Alphabet(fst.alphabet.getSymbols());

            s = -1;
            t = -1;
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
            return s+1 < fst.transitions.size();
        }

        @Override
        public void nextState() {
            s++;
            t = -1;
            stateTrans = fst.transitions.get(s);
        }

        @Override
        public boolean accepting() {
            return fst.accepting.get(s);
        }

        @Override
        public boolean hasNextTransition() {
            return t+1 < stateTrans.size();
        }

        @Override
        public void nextTransition() {
            t++;
        }

        @Override
        public boolean identity() {
            return stateTrans.get(t).identity(fst.idIdx);
        }

        @Override
        public int inId() {
            if (identity())
                return -1;
            else
                return stateTrans.get(t).getInSym();
        }

        @Override
        public int outId() {
            if (identity())
                return -1;
            else
                return stateTrans.get(t).getOutSym();
        }

        @Override
        public int toId() {
            return stateTrans.get(t).getToState();
        }
    }

}
