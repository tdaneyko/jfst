package de.tuebingen.sfs.jfst.transduce.compact;

import de.tuebingen.sfs.jfst.symbol.Alphabet;
import de.tuebingen.sfs.jfst.io.*;
import de.tuebingen.sfs.jfst.transduce.MutableTransducer;
import de.tuebingen.sfs.jfst.transduce.StateIterator;
import de.tuebingen.sfs.jfst.transduce.Transducer;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

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

    // True if this property holds for sure (false != property does not hold!)
    private boolean epsilonFree;
    private boolean deterministic;
    private boolean noUnreachableStates;
    private boolean minimal;

    private void reset() {
        this.start = -1;
        this.transitions = new ArrayList<>();
        this.accepting = new ArrayList<>();
        this.s = 0;

        this.epsilonFree = true;
        this.deterministic = true;
        this.noUnreachableStates = true;
        this.minimal = true;
    }

    public MutableCompactTransducer() {
        this.alphabet = new Alphabet();
        this.transitions = new ArrayList<>();
        this.accepting = new ArrayList<>();
        this.start = addState(false);

        this.epsilonFree = true;
        this.deterministic = true;
        this.noUnreachableStates = true;
        this.minimal = true;
    }

    public MutableCompactTransducer(StateIterator iter) {
        // Initialize state and transition lists
        reset();

        // Copy alphabet
        this.alphabet = iter.getAlphabet();

        // Store states and transitions
        appendStatesAndTransitions(iter, 0, null);

        // Set start state
        this.start = iter.getStartState();
    }

    private void appendStatesAndTransitions(StateIterator iter, int stateOffset, int[] alphTransformations) {
        while (iter.hasNextState()) {
            iter.nextState();
            List<CompactTransition> stateTrans = new ArrayList<>();
            accepting.add(iter.accepting());
            while (iter.hasNextTransition()) {
                iter.nextTransition();
                int inId = iter.inId();
                int outId = iter.outId();
                if (alphTransformations != null) {
                    inId = alphTransformations[inId];
                    outId = alphTransformations[outId];
                }
                stateTrans.add(new CompactTransition(inId, outId, iter.toId() + stateOffset));
                if (alphabet.epsilon(inId) && alphabet.epsilon(outId))
                    epsilonFree = false;
            }
            // Sort transitions for current state
            Collections.sort(stateTrans);
            transitions.add(stateTrans);
            s++;
        }

        deterministic = false;
        noUnreachableStates = false;
        minimal = false;
    }

    @Override
    public int addState(boolean accepting) {
        this.transitions.add(new ArrayList<>());
        this.accepting.add(accepting);
        this.noUnreachableStates = false;
        this.minimal = false;
        return s++;
    }

    @Override
    public void setAccepting(int stateId, boolean accepting) {
        this.accepting.set(stateId, accepting);
    }

    @Override
    public void addTransition(int from, String inSym, String outSym, int to) {
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
        if (epsilonFree && transition.epsilon(alphabet.epsilonId()))
            epsilonFree = false;
        deterministic = false;
        minimal = false;
    }

    private void addTransitions(int from, Iterable<CompactTransition> transitions) {
        for (CompactTransition trans : transitions)
            addTransition(from, trans);
    }

    @Override
    public void addEpsilonTransition(int from, int to) {
        epsilonFree = false;
        super.addEpsilonTransition(from, to);
    }

    /**
     * Parse a file in AT&amp;T format into a MutableCompactTransducer.
     * @param in AT&amp;T file
     * @param producer Original producer of the file
     * @return The Transducer specified by the file
     */
    public static MutableCompactTransducer readFromATT(InputStream in, FstProducer producer) {
        return readFromATT(in, producer, false);
    }

    /**
     * Parse a file in AT&amp;T format into a MutableCompactTransducer.
     * @param in AT&amp;T file
     * @param producer Original producer of the file
     * @param inverse False: Input symbol comes before output symbol; True: Output symbol comes before input symbol
     * @return The Transducer specified by the file
     */
    public static MutableCompactTransducer readFromATT(InputStream in, FstProducer producer, boolean inverse) {
//        return MutableFSTOld.readFromATT(in, producer, inverse).makeCompact();
        return new MutableCompactTransducer(new AttFileStateIterator(in, producer, inverse));
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

    public boolean isEpsilonFree() {
        return epsilonFree;
    }

    public boolean isDeterministic() {
        return deterministic;
    }

    public boolean checkIsDeterministic() {
        if (deterministic)
            return true;

        Set<Set<Integer>> stateSets = new HashSet<>();
        Set<Integer> startSet = new HashSet<>();
        startSet.add(start);
        Set<Integer> immStartSet = Collections.unmodifiableSet(startSet);

        stateSets.add(immStartSet);

        LinkedHashSet<Set<Integer>> queue = new LinkedHashSet<>();
        queue.add(immStartSet);
        while (!queue.isEmpty()) {
            Set<Integer> currentStateSet = queue.iterator().next();
            queue.remove(currentStateSet);

            // Collect outgoing transitions of state set members
            Map<Long, Set<Integer>> setTrans = new TreeMap<>();
            for (Integer from : currentStateSet) {
                for (CompactTransition trans : transitions.get(from)) {
                    int to = trans.getToState();
                    long syms = trans.getInternalRepresentation() & (~CompactTransition.GET_TO_STATE);
                    Set<Integer> toStates = setTrans.computeIfAbsent(syms, x -> new HashSet<>());
                    toStates.add(to);
                }
            }

            // Enqueue new state sets
            stateSets.addAll(setTrans.values());
            queue.addAll(setTrans.values());
        }

        deterministic = stateSets.size() == nOfStates();
        return deterministic;
    }

    public boolean hasNoUnreachableStates() {
        return noUnreachableStates;
    }

    public boolean checkHasNoUnreachableStates() {
        noUnreachableStates = noUnreachableStates || deterministic || minimal
                || (getReachableStates().size() == nOfStates());
        return noUnreachableStates;
    }

    public boolean isMinimal() {
        return minimal;
    }

    public boolean checkIsMinimal() {
        minimal = minimal || isEpsilonFree() && checkIsDeterministic() && checkHasNoUnreachableStates()
                && (hopcroftAlgorithm().size() == nOfStates());
        return minimal;
    }

    @Override
    public StateIterator iter() {
        return new MutableCompactTransducerStateIterator(this);
    }

    public void removeEpsilons() {
        if (epsilonFree)
            return;

        Set<Integer> done = new HashSet<>();
        for (int state = 0; state < transitions.size(); state++) {
            if (!done.contains(state)) {
                done.add(state);
                done.addAll(removeEpsilons(state));
            }
        }
        epsilonFree = true;
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
        if (deterministic)
            return;

        this.removeEpsilons();

        List<List<CompactTransition>> oldTrans = transitions;
        List<Boolean> oldAcc = accepting;

        Map<Set<Integer>, Integer> stateSets = new HashMap<>();
        Set<Integer> startSet = new HashSet<>();
        startSet.add(start);
        Set<Integer> immStartSet = Collections.unmodifiableSet(startSet);

        reset();
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

        this.deterministic = true;
        this.noUnreachableStates = true;
    }

    public void removeUnreachableStates() {
        if (noUnreachableStates)
            return;

        Set<Integer> reachables = getReachableStates();
        if (reachables.size() < nOfStates()) {
            int[] stateTransformations = new int[nOfStates()];
            int currentOffset = 0;
            for (int s = 0; s < nOfStates(); s++) {
                if (reachables.contains(s))
                    stateTransformations[s] = s - currentOffset;
                else {
                    transitions.remove(s - currentOffset);
                    accepting.remove(s - currentOffset);
                    currentOffset++;
                }
            }
            this.s = reachables.size();

            for (List<CompactTransition> stateTrans : transitions) {
                for (CompactTransition trans : stateTrans) {
                    trans.setToState(stateTransformations[trans.getToState()]);
                }
            }
        }

        noUnreachableStates = true;
    }

    private Set<Integer> getReachableStates() {
        return transitions.stream()
                .map(tList -> tList.stream().map(CompactTransition::getToState))
                .flatMap(toStream -> toStream)
                .collect(Collectors.toSet());
    }

    @Override
    // TODO: Make more efficient
    public void minimize() {
        if (minimal)
            return;

        if (!deterministic)
            this.determinize();
        if (!noUnreachableStates)
            this.removeUnreachableStates();

        Set<Set<Integer>> equivSets = hopcroftAlgorithm();
        if (equivSets.size() < nOfStates()) {
            int[] stateTransformations = new int[nOfStates()];
            int oldStart = start;
            List<List<CompactTransition>> oldTrans = transitions;
            List<Boolean> oldAcc = accepting;
            reset();
            start = stateTransformations[oldStart];
            // Create new states
            for (Set<Integer> equivStates : equivSets) {
                int newState = addState();
                for (int oldState : equivStates) {
                    stateTransformations[oldState] = newState;
                    setAccepting(newState, oldAcc.get(oldState));
                }
            }
            // Update transitions
            for (Set<Integer> equivStates : equivSets) {
                for (int oldState : equivStates) {
                    int newState = stateTransformations[oldState];
                    for (CompactTransition trans : oldTrans.get(oldState)) {
                        trans.setToState(stateTransformations[trans.getToState()]);
                        addTransition(newState, trans);
                    }
                }
            }
        }

        minimal = true;
    }

    private Set<Set<Integer>> hopcroftAlgorithm() {
        Set<Integer> finalStates = new HashSet<>();
        Set<Integer> nonFinalStates = new HashSet<>();
        for (int s = 0; s < nOfStates(); s++)
            ((accepting.get(s)) ? finalStates : nonFinalStates).add(s);

        Set<Set<Integer>> equivSets = new HashSet<>();
        equivSets.add(finalStates);
        equivSets.add(nonFinalStates);
        Set<Set<Integer>> candidateSets = new LinkedHashSet<>();
        candidateSets.add(finalStates);
        candidateSets.add(nonFinalStates);

        while (!candidateSets.isEmpty()) {
            Set<Integer> a = candidateSets.iterator().next();
            candidateSets.remove(a);
            for (int inSym = 0; inSym < alphabet.size(); inSym++) {
                for (int outSym = 0; outSym < alphabet.size(); outSym++) {
                    Set<Integer> fromStates = getFromStates(inSym, outSym, a);
                    Set<Set<Integer>> newEquivSets = new HashSet<>(equivSets);
                    for (Set<Integer> equivStates : equivSets) {
                        Set<Integer> intersection = new HashSet<>();
                        Set<Integer> difference = new HashSet<>(equivStates);
                        for (int from : fromStates) {
                            if (difference.remove(from))
                                intersection.add(from);
                        }
                        if (!intersection.isEmpty() && !difference.isEmpty()) {
                            newEquivSets.remove(equivStates);
                            newEquivSets.add(intersection);
                            newEquivSets.add(difference);
                            if (candidateSets.remove(equivStates)) {
                                candidateSets.add(intersection);
                                candidateSets.add(difference);
                            }
                            else if (intersection.size() <= difference.size())
                                candidateSets.add(intersection);
                            else
                                candidateSets.add(difference);
                        }
                    }
                    equivSets = newEquivSets;
                }
            }
        }

        return equivSets;
    }

    private Set<Integer> getFromStates(int inSym, int outSym, Set<Integer> toStates) {
        Set<Integer> fromStates = new HashSet<>();
        Set<CompactTransition> possibleTransitions = new TreeSet<>();
        for (int to : toStates)
            possibleTransitions.add(new CompactTransition(inSym, outSym, to));

        for (int s = 0; s < nOfStates(); s++) {
            if (!Collections.disjoint(transitions.get(s), possibleTransitions))
                fromStates.add(s);
        }

        return fromStates;
    }

    @Override
    public void repeat(int n) {
        if (n == 0) {
            this.transitions = new ArrayList<>();
            this.accepting = new ArrayList<>();
            this.start = addState(true);
        }
        else if (n == 2)
            this.concat(this);
        else if (n > 2) {
            int singleSize = nOfStates();
            List<List<CompactTransition>> singleTransitions = transitions.stream()
                    .map(Collections::unmodifiableList).collect(Collectors.toList());
            List<Boolean> singleAccepting = Collections.nCopies(singleSize, false);

            for (int i = 1; i < n; i++)
                this.mergeMutableTransducersWithSameAlphabet(singleTransitions, singleAccepting);

            for (int s = 0; s < singleSize; s++) {
                if (accepting.get(s)) {
                    for (int i = 1; i < n; i++) {
                        addEpsilonTransition(i * singleSize + s, (i-1) * singleSize + start);
                    }
                }
            }

            start = (n-1) * singleSize + start;
        }

        deterministic = false;
        minimal = false;
    }

    @Override
    public void repeatMin(int n) {
        int singleSize = nOfStates();
        int lastStart = start;

        if (n > 1)
            this.repeat(n);

        for (int s = 0; s < singleSize; s++) {
            if (accepting.get(s)) {
                addEpsilonTransition(s, lastStart);
            }
        }

        if (n == 0)
            setAccepting(start, true);

        deterministic = false;
        minimal = false;
    }

    @Override
    public void optional() {
        setAccepting(start, true);
        minimal = false;
    }

    @Override
    public void inverse() {
        for (List<CompactTransition> stateTrans : transitions) {
            for (CompactTransition trans : stateTrans)
                trans.invert();
        }
    }

    @Override
    public void reverse() {
        int oldStart = start;
        List<List<CompactTransition>> oldTrans = transitions;

        start = addState(isAccepting(oldStart));
        transitions = new ArrayList<>();
        for (int s = 0; s < accepting.size(); s++)
            transitions.add(new ArrayList<>());

        for (int s = 0; s < oldTrans.size(); s++) {
            List<CompactTransition> stateTrans = oldTrans.get(s);
            for (CompactTransition trans : stateTrans) {
                addTransition(trans.getToState(),
                        new CompactTransition(trans.getInSym(), trans.getOutSym(), s));
            }
            if (isAccepting(s)) {
                addEpsilonTransition(start, s);
                setAccepting(s, false);
            }
        }

        setAccepting(oldStart, true);

        deterministic = false;
        noUnreachableStates = false;
        minimal = false;
    }

    private int[] mergeAlphabets(Alphabet other) {
        if (Arrays.equals(alphabet.getSymbols(), other.getSymbols()))
            return null;

        int[] otherIdsToNew = new int[other.size()];
        for (int a = 0; a < other.size(); a++) {
            int id = alphabet.getIdOrCreate(other.getSymbol(a));
            otherIdsToNew[a] = id;
        }
        return otherIdsToNew;
    }

    private int mergeTransducers(Transducer other) {
        int[] alphTransformations = mergeAlphabets(other.getAlphabet());

        if (alphTransformations == null && other instanceof MutableCompactTransducer) {
            MutableCompactTransducer otherMut = (MutableCompactTransducer) other;
            mergeMutableTransducersWithSameAlphabet(otherMut.transitions, otherMut.accepting);
            return otherMut.start;
        }
        else {
            StateIterator iter = other.iter();
            int offset = nOfStates();
            appendStatesAndTransitions(iter, offset, alphTransformations);
            return offset + iter.getStartState();
        }
    }

    private void mergeMutableTransducersWithSameAlphabet(
            List<List<CompactTransition>> transitions, List<Boolean> accepting) {
        final int offset = nOfStates();
        this.transitions.addAll(transitions.stream().map(tList ->
                tList.stream()
                        .map(t -> new CompactTransition(t.getInternalRepresentation() + offset))
                        .collect(Collectors.toList()))
                .collect(Collectors.toList()));
        this.accepting.addAll(accepting);
    }

    @Override
    public void concat(Transducer other) {
        int startOfAppended = mergeTransducers(other);
        for (int s = 0; s < startOfAppended; s++) {
            if (accepting.get(s)) {
                addEpsilonTransition(s, startOfAppended);
                setAccepting(s, false);
            }
        }

        deterministic = false;
        noUnreachableStates = false;
        minimal = false;
    }

    @Override
    public void union(Transducer other) {
        int oldStart = start;
        int otherStart = mergeTransducers(other);
        start = addState();
        addEpsilonTransition(start, oldStart);
        addEpsilonTransition(start, otherStart);

        deterministic = false;
        noUnreachableStates = false;
        minimal = false;
    }

    @Override
    public void priorityUnion(Transducer other) {
        // TODO: Implement
    }

    @Override
    public void intersect(Transducer other) {
        // TODO: Implement
    }

    @Override
    public void subtract(Transducer other) {
        // TODO: Implement
    }

    @Override
    public void compose(Transducer other) {
        // TODO: Implement
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
