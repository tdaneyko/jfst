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

    private int addState() {
        return addState(false);
    }

    private int addState(boolean accepting) {
        this.transitions.add(new ArrayList<>());
        this.accepting.add(accepting);
        return s++;
    }

    private void setAccepting(int stateId, boolean accepting) {
        this.accepting.set(stateId, accepting);
    }

    private void addTransition(int from, String inSym, String outSym, int to) {
        addTransition(from, new CompactTransition(
                alphabet.getIdOrCreate(inSym), alphabet.getIdOrCreate(outSym), to));
    }

    private void addTransition(int from, CompactTransition transition) {
        List<CompactTransition> trans = transitions.get(from);
        int i = Collections.binarySearch(trans, transition);
        if (i < 0) {
            i = -(i + 1);
            trans.add(i, transition);
        }
    }

    private void addTransitions(int from, Iterable<CompactTransition> transitions) {
        for (CompactTransition trans : transitions)
            addTransition(from, trans);
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
        return new MutableCompactTransducerStateIterator(this);
    }

    public void removeEpsilons() {
        Set<Integer> done = new HashSet<>();
        for (int state = 0; state < transitions.size(); state++) {
            if (!done.contains(state)) {
                done.add(state);
                done.addAll(removeEpsilons(state));
            }
        }
    }

    private Set<Integer> removeEpsilons(int state) {
        Set<Integer> reachables = new HashSet<>();
        List<CompactTransition> stateTrans = transitions.get(state);
        int i = Collections.binarySearch(stateTrans, ((long) alphabet.epsilonId()) << 48);
        if (i < 0)
            i = -(i + 1);
        while (i < stateTrans.size()) {
            CompactTransition trans = stateTrans.get(i);
            if (trans.getInSym() == alphabet.epsilonId()) {
                if (trans.getOutSym() == alphabet.epsilonId()) {
                    reachables.add(trans.getToState());
                    reachables.addAll(removeEpsilons(trans.getToState()));
                    stateTrans.remove(i);
                }
                else
                    i++;
            }
            else
                break;
        }

        for (int reachable : reachables) {
            if (isAccepting(reachable))
                setAccepting(state, true);
            addTransitions(state, transitions.get(reachable));
        }

        return reachables;
    }

    @Override
    public void determinize() {
        removeEpsilons();

        List<List<CompactTransition>> oldTrans = transitions;
        List<Boolean> oldAcc = accepting;

        Map<Set<Integer>, Integer> stateSets = new HashMap<>();
        Set<Integer> startSet = new HashSet<>();
        startSet.add(start);
        Set<Integer> immStartSet = Collections.unmodifiableSet(startSet);

        this.s = 0;
        this.transitions = new ArrayList<>();
        this.accepting = new ArrayList<>();
        this.start = addState();
        stateSets.put(immStartSet, start);

        LinkedHashSet<Set<Integer>> queue = new LinkedHashSet<>();
        queue.add(immStartSet);
        while (!queue.isEmpty()) {
            Set<Integer> currentStateSet = queue.iterator().next();
            queue.remove(currentStateSet);
            int currentStateId = stateSets.get(currentStateSet);

            // Collect outgoing transitions of state set members
            Map<Long, Set<Integer>> setTrans = new TreeMap<>();
            boolean acc = false;
            for (Integer from : currentStateSet) {
                acc = acc || oldAcc.get(from); // If any state in set is accepting, the whole set is accepting
                for (CompactTransition trans : oldTrans.get(from)) {
                    int to = trans.getToState();
                    long syms = trans.getInternalRepresentation() & (~CompactTransition.GET_TO_STATE);
                    Set<Integer> toStates = setTrans.computeIfAbsent(syms, x -> new HashSet<>());
                    toStates.add(to);
                }
            }
            setAccepting(currentStateId, acc);

            // Set new transitions and create new states
            for (Map.Entry<Long, Set<Integer>> entry : setTrans.entrySet()) {
                int i = stateSets.getOrDefault(entry.getValue(), -1);
                if (i == -1) {
                    i = addState();
                    Set<Integer> newStateSet = Collections.unmodifiableSet(entry.getValue());
                    stateSets.put(newStateSet, i);
                    queue.add(newStateSet);
                }
                CompactTransition trans = new CompactTransition(entry.getKey() | ((long) i));
                addTransition(currentStateId, trans);
            }
        }
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

    private static class MutableCompactTransducerStateIterator implements StateIterator {

        final MutableCompactTransducer fst;
        final Alphabet alphabet;

        int s;
        int t;

        List<CompactTransition> stateTrans;

        public MutableCompactTransducerStateIterator(MutableCompactTransducer fst) {
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
