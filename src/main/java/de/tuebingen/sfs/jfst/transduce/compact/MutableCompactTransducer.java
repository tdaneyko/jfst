package de.tuebingen.sfs.jfst.transduce.compact;

import de.tuebingen.sfs.jfst.symbol.Alphabet;
import de.tuebingen.sfs.jfst.io.*;
import de.tuebingen.sfs.jfst.transduce.MutableTransducer;
import de.tuebingen.sfs.jfst.transduce.StateIterator;
import de.tuebingen.sfs.jfst.transduce.Transducer;

import java.io.InputStream;
import java.util.*;

/**
 * A mutable Transducer to which new states and transitions may be added.
 */
public class MutableCompactTransducer extends MutableTransducer {

    // Literal symbols used by the transliterator
    private Alphabet alphabet;

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

    public MutableCompactTransducer() {
        this.alphabet = new Alphabet();
        this.transitions = new ArrayList<>();
        this.accepting = new ArrayList<>();
        this.start = addState(false);
    }

    public MutableCompactTransducer(StateIterator iter) {
        // Set start state
        this.start = iter.getStartState();
        // Copy alphabet
        this.alphabet = iter.getAlphabet();

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
     * Parse a file in AT&amp;T format into a CompactTransducer.
     * Calls MutableFSTOld.readFromATT(in, producer).makeCompact() internally.
     * @param in AT&amp;T file
     * @param producer Original producer of the file
     * @return The Transducer specified by the file
     */
    public static MutableCompactTransducer readFromATT(InputStream in, FstProducer producer) {
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
    public static MutableCompactTransducer readFromATT(InputStream in, FstProducer producer, boolean reverse) {
//        return MutableFSTOld.readFromATT(in, producer, reverse).makeCompact();
        return new MutableCompactTransducer(new AttFileStateIterator(in, producer, reverse));
    }

    /**
     * Load a Compact Transducer from a binary JFST file.
     * @param fileName The path to the JFST file
     * @return The Transducer specified by the file
     */
    public static MutableCompactTransducer readFromBinary(String fileName) {
        return readFromBinary(fileName, false);
    }

    /**
     * Load a Compact Transducer from a binary Transducer file.
     * @param fileName The path to the Transducer file
     * @param producer Original producer of the file
     * @return The Transducer specified by the file
     */
    public static MutableCompactTransducer readFromBinary(String fileName, FstProducer producer) {
        return readFromBinary(fileName, producer, false);
    }

    /**
     * Load a Compact Transducer from a binary JFST file.
     * @param fileName The path to the JFST file
     * @param inverse If true, invert input and output symbols
     * @return The Transducer specified by the file
     */
    public static MutableCompactTransducer readFromBinary(String fileName, boolean inverse) {
        FstProducer producer = (fileName.endsWith(".hfst")) ? FstProducer.HFST : FstProducer.JFST;
        return readFromBinary(fileName, producer, inverse);
    }

    /**
     * Load a Compact Transducer from a binary Transducer file.
     * @param fileName The path to the Transducer file
     * @param producer Original producer of the file
     * @param inverse If true, invert input and output symbols
     * @return The Transducer specified by the file
     */
    public static MutableCompactTransducer readFromBinary(String fileName, FstProducer producer, boolean inverse) {
        if (producer.equals(FstProducer.SFST)) {
            System.err.println("Cannot read SFST binary files (yet).");
            return null;
        }
        FileStateIterator iter = (producer.equals(FstProducer.JFST))
                ? new JfstFileStateIterator(fileName, inverse)
                : new HfstFileStateIterator(fileName, inverse);
        MutableCompactTransducer fst = new MutableCompactTransducer(iter);
        iter.close();
        return fst;
    }

    /**
     * @return The compact version of this Transducer
     */
    public CompactTransducer makeCompact() {
        return new CompactTransducer(this.iter());
    }

    @Override
    public int getStartState() {
        return start;
    }

    @Override
    public boolean isAccepting(int stateId) {
        return accepting.get(stateId);
    }

    @Override
    public Iterator<Transition> getTransitionIterator(int statIdx) {
        return new TransitionIterator(statIdx);
    }

    @Override
    public Iterator<Transition> getTransitionIterator(String s, int statIdx) {
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
    public StateIterator iter() {
        return new CompactFSTStateIterator(this);
    }

    @Override
    public void determinize() {

    }

    @Override
    public void minimize() {

    }

    @Override
    public void repeat(int n) {

    }

    @Override
    public void repeatMin(int n) {

    }

    @Override
    public void optional() {

    }

    @Override
    public void invert() {

    }

    @Override
    public void reverse() {

    }

    @Override
    public void concat(Transducer other) {

    }

    @Override
    public void union(Transducer other) {

    }

    @Override
    public void priorityUnion(Transducer other) {

    }

    @Override
    public void intersect(Transducer other) {

    }

    @Override
    public void compose(Transducer other) {

    }

    @Override
    public void subtract(Transducer other) {

    }

    private class TransitionIterator implements Iterator<Transition> {

        private final String inC;
        private final long inSym;

        private int i;
        private final List<CompactTransition> stateTrans;

        public TransitionIterator(String s, int statIdx) {
            inC = s;
            if (alphabet.contains(s)) {
                long ci = alphabet.getId(s);
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
                    (stateTrans.get(i).getInternalRepresentation() & CompactTransition.GET_IN_SYM) == inSym);
        }

        @Override
        public Transition next() {
            CompactTransition transition = stateTrans.get(i);
            int toState = transition.getToState();
            int outIdx = transition.getOutSym();
            String out = (outIdx == alphabet.identityId()) ? inC : alphabet.getSymbol(outIdx).toString();
            String in = (inSym == 1) ? alphabet.getSymbol(transition.getInSym()).toString() : inC;
            i++;
            return new Transition(toState, in, out);
        }
    }

    private static class CompactFSTStateIterator implements StateIterator {

        final MutableCompactTransducer fst;
        final Alphabet alphabet;

        int s;
        int t;

        List<CompactTransition> stateTrans;

        public CompactFSTStateIterator(MutableCompactTransducer fst) {
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
        public int inId() {
            return stateTrans.get(t).getInSym();
        }

        @Override
        public int outId() {
            return stateTrans.get(t).getOutSym();
        }

        @Override
        public int toId() {
            return stateTrans.get(t).getToState();
        }
    }

}
