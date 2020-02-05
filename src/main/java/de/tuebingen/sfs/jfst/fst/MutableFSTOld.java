package de.tuebingen.sfs.jfst.fst;

import de.tuebingen.sfs.jfst.io.ATTWriter;
import de.tuebingen.sfs.jfst.io.FSTProducer;
import de.tuebingen.sfs.jfst.alphabet.Alphabet;
import de.tuebingen.sfs.jfst.alphabet.Symbol;
import de.tuebingen.sfs.jfst.io.BinaryFSTWriter;
import de.tuebingen.sfs.util.string.StringUtils;
import de.tuebingen.sfs.util.bin.BufferedByteReader;
import de.tuebingen.sfs.util.bin.IOUtils;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * A mutable FST to which new states and transitions may be added.
 */
public class MutableFSTOld extends FST {

    // Start state
    private MutableState start;
    // Literal symbols used by the transliterator
    private Alphabet alphabet;
    // States of the transliterator
    private List<MutableState> states;
    // The number of transitions in the transliterator
    private int nTrans;

    /**
     * Parse a file in AT&amp;T format into a MutableFSTOld.
     * @param in AT&amp;T file
     * @param producer Original producer of the file
     * @return The FST specified by the file
     */
    public static MutableFSTOld readFromATT(InputStream in, FSTProducer producer) {
        return readFromATT(in, producer, false);
    }

    /**
     * Parse a file in AT&amp;T format into a MutableFSTOld.
     * @param in AT&amp;T file
     * @param producer Original producer of the file
     * @param reverse False: Input symbol comes before output symbol; True: Output symbol comes before input symbol
     * @return The FST specified by the file
     */
    public static MutableFSTOld readFromATT(InputStream in, FSTProducer producer, boolean reverse) {
        return new MutableFSTOld(in, producer, reverse);
    }

    /**
     * Load a Mutable FST from a binary JFST file.
     * @param in The JFST file
     * @return The FST specified by the file
     * @throws IOException
     */
    public static MutableFSTOld readFromBinary(InputStream in) throws IOException {
        return new MutableFSTOld(in, false);
    }

    public static MutableFSTOld readFromBinary(InputStream in, boolean inverse) throws IOException {
        return new MutableFSTOld(in, inverse);
    }

    private MutableFSTOld() {
        start = null;
        alphabet = new Alphabet();
        states = new ArrayList<>();
        nTrans = 0;
    }

    // Create FST from AT&T
    public MutableFSTOld(InputStream in, FSTProducer producer, boolean inverse) {
        alphabet = new Alphabet();
        nTrans = 0;
        String identity = producer.identity();
        try (BufferedReader read = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            // Id-to-state mapping - stored in a map to be able to add arbitrary states occurring
            // in transitions out of order
            TIntObjectMap<MutableState> states = new TIntObjectHashMap<>();
            for (String line = read.readLine(); line != null; line = read.readLine()) {
                if (!line.isEmpty()) {
                    String[] fields = StringUtils.split(line, '\t');
                    if (fields.length > 0) {
                        // Get or create state with id
                        int id = Integer.parseInt(fields[0]);
                        if (!states.containsKey(id))
                            states.put(id, new MutableState(false, id));
                        MutableState state = states.get(id);
                        // Set accepting...
                        if (fields.length == 1 || fields.length == 2)
                            state.setAccepting(true);
                        // ...or add transition
                        else if (fields.length == 4 || fields.length == 5) {
                            int toId = Integer.parseInt(fields[1]);
                            String inSym = (inverse) ? fields[3] : fields[2];
                            String outSym = (inverse) ? fields[2] : fields[3];
                            // Get or create to-state
                            if (!states.containsKey(toId))
                                states.put(toId, new MutableState(false, toId));
                            MutableState toState = states.get(toId);
                            // Create identity transition
                            if (inSym.equals(identity) && outSym.equals(identity))
                                state.addIdentityArc(toState);
                            // Create literal transition
                            else
                                state.addLiteralArc(alphabet.getSymbol(producer.convert(inSym)),
                                        alphabet.getSymbol(producer.convert(outSym)),
                                        toState);
                            nTrans++;
                        }
                    }
                }
            }
            // Create list of states
            this.start = states.get(0);
            int z = states.size();
            this.states = new ArrayList<>(z);
            for (int i = 0; i < z; i++) {
                MutableState s = states.get(i);
                if (s == null)
                    z++;
                this.states.add(s);
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Create FST from binary
    private MutableFSTOld(InputStream in, boolean inverse) throws IOException {
        int bufferSize = 8192;
        BufferedByteReader reader = new BufferedByteReader(in, bufferSize);

        boolean alph = true; // Currently reading alphabet
        boolean litTrans = false; // Currently reading literal transitions
        // If both false: Currently reading identity transitions

        alphabet = new Alphabet();
        // Create byte representation of newline char
        int nli = '\n';
        byte[] newline = new byte[2];
        newline[1] = (byte) nli;
        newline[0] = (byte) (nli >> 8);

        // Create alphabet
        StringBuilder sym = new StringBuilder();
        while (alph && reader.hasNext()) {
            if (reader.startsWith(newline)) {
                // Double newline => end of alphabet
                if (sym.length() == 0) {
                    alph = false;
                    litTrans = true;
                }
                // Single newline => end of symbol
                else {
                    alphabet.addSymbol(sym.toString());
                    sym = new StringBuilder();
                }
                // Skip over newline
                reader.skip(2);
            }
            else {
                // Append non-newline char
                int c = reader.popToInt(2);
                sym.append((char) c);
            }
        }

        int nStates = reader.popToInt();
        int s = IOUtils.bytesNeededFor(nStates-1); // State id size
        int startID = reader.popToInt(s);
        nTrans = reader.popToInt();
        int a = IOUtils.bytesNeededFor(alphabet.size()-1); // Symbol id size
        // Create empty states
        states = new ArrayList<>(nStates);
        for (int i = 0; i < nStates; i++)
            states.add(new MutableState(false, i));
        MutableState current = states.get(0); // Current state to parse
        start = states.get(startID);
        int sid = 1; // Id of next state

        // Control bytes
        final byte accepting = BinaryFSTWriter.ACCEPTING;
        final byte nonaccepting = BinaryFSTWriter.NONACCEPTING;
        final byte stateend = BinaryFSTWriter.STATEEND;

        int litSize = s + a + a; // Byte length of a literal transition
        boolean eof = false; // Reached end of file?

        while (!eof && reader.hasNext()) {
            // In literal transition area
            if (litTrans) {
                // End of literal transitions
                if (reader.startsWith(accepting) || reader.startsWith(nonaccepting)) {
                    if (reader.startsWith(accepting))
                        current.accepting = true;
                    litTrans = false;
                    reader.skip(1);
                }
                // Add literal transition
                else if (reader.hasNext(litSize)) {
                    int toId = reader.popToInt(s);
                    int inSym = reader.popToInt(a);
                    int outSym = reader.popToInt(a);
                    if (inverse) {
                        int tmp = inSym;
                        inSym = outSym;
                        outSym = tmp;
                    }
                    current.addLiteralArc(alphabet.getSymbol(inSym),
                            alphabet.getSymbol(outSym),
                            states.get(toId));
                }
                // If there are non enough bytes for a literal transition and the end of literals is not marked,
                // the end of a valid binary file is reached.
                else
                    eof = true;
            }
            // In identity transition area
            else {
                // Reached end of state
                if (reader.startsWith(stateend)) {
                    // Back to literal transition area
                    litTrans = true;
                    // If this was the last state => end of file
                    if (sid == states.size())
                        eof = true;
                    // Else process next state
                    else if (sid < states.size()) {
                        current = states.get(sid);
                        sid++;
                    }
                    // Else: weird unexplainable stuff happening
                    else
                        System.err.println("More states listed than declared initially.");
                    reader.skip(1);
                }
                // Add identity transition
                else if (reader.hasNext(s)) {
                    int toId = reader.popToInt(s);
                    current.addIdentityArc(states.get(toId));
                }
                // If there are non enough bytes for an identity transition and the end of identities is not marked,
                // the end of a valid binary file is reached.
                else
                    eof = true;
            }
        }
    }

    public MutableFSTOld invert() {
        for (MutableState state : states) {
            state.invert();
        }
        return this;
    }

    /**
     * @return The compact version of this FST
     */
    public CompactFST makeCompact() {
        return new CompactFST(this.iter());
    }

    @Override
    public int nOfStates() {
        return states.size();
    }

    @Override
    public int nOfTransitions() {
        return nTrans;
    }

    @Override
    public String[] getSymbols() {
        return alphabet.getSymbols();
    }

    @Override
    public MutableFSTStateIterator iter() {
        return new MutableFSTStateIterator(this);
    }

    @Override
    public Set<String> apply(String in, Iterable<String> ignoreInInput) {
        return start.apply(in, 0, ignoreInInput);
    }

    @Override
    public Set<String> prefixSearch(String prefix, int maxSuffix, Iterable<String> ignoreInInput) {
        return start.prefixSearch(prefix, 0, ignoreInInput, maxSuffix);
    }


    private class MutableState {

        // Input symbols of literal transitions, ordered
        private final List<Symbol> inSyms;
        // Output symbols corresponding to the input symbols
        private final List<Symbol> outSyms;
        // To-states corresponding to the input symbols
        private final List<MutableState> toStates;

        // To-states of identity transitions
        private final List<MutableState> idToStates;

        // State accepting?
        private boolean accepting;

        // State id
        private final int id;


        public MutableState(boolean accepting, int id) {
            this.inSyms = new ArrayList<>();
            this.outSyms = new ArrayList<>();
            this.toStates = new ArrayList<>();
            this.idToStates = new ArrayList<>();
            this.accepting = accepting;
            this.id = id;
        }

        /**
         * Set whether the state is an accepting state
         * @param accepting True if state is accepting
         */
        public void setAccepting(boolean accepting) {
            this.accepting = accepting;
        }

        /**
         * Add a literal transition to another state.
         * @param in Input symbol
         * @param out Output symbol
         * @param to To-state
         */
        public void addLiteralArc(Symbol in, Symbol out, MutableState to) {
            int i = Collections.binarySearch(inSyms, in);
            if (i < 0)
                i = -i - 1;
            inSyms.add(i, in);
            outSyms.add(i, out);
            toStates.add(i, to);
        }

        /**
         * Add an identity transition to another state.
         * @param to To-state
         */
        public void addIdentityArc(MutableState to) {
            idToStates.add(to);
        }

        public void invert() {
            List<Symbol> inSymsOld = new ArrayList<>(inSyms);
            List<Symbol> outSymsOld = new ArrayList<>(outSyms);
            List<MutableState> toStatesOld = new ArrayList<>(toStates);
            inSyms.clear();
            outSyms.clear();
            toStates.clear();
            for (int i = 0; i < inSymsOld.size(); i++)
                addLiteralArc(outSymsOld.get(i), inSymsOld.get(i), toStatesOld.get(i));
        }

        /**
         * Apply the transducer starting at this state to an input string starting at index i.
         * @param s The input string
         * @param i Current position in the string
         * @return The output strings matched to the input string by this transducer
         */
        public Set<String> apply(String s, int i) {
            return apply(s, i, null);
        }

        /**
         * Apply the transducer starting at this state to an input string starting at index i.
         * @param s The input string
         * @param i Current position in the string
         * @param ignoreInInput Also take transitions with these input symbols even if they do not occur in the input string
         * @return The output strings matched to the input string by this transducer
         */
        public Set<String> apply(String s, int i, Iterable<String> ignoreInInput) {
            // String has been consumed?
            boolean sFin = i >= s.length();

            // Result set
            Set<String> res = new HashSet<>();

            // Return empty string if accepting
            if (sFin && accepting)
                res.add("");

            // Apply ignore transitions
            if (ignoreInInput != null) {
                for (String ign : ignoreInInput) {
                    int start = firstIndexOf(inSyms, ign);
                    if (start >= 0) {
                        for (int j = start; j < inSyms.size() && inSyms.get(j).equivalentTo(ign); j++) {
                            Symbol out = outSyms.get(j);
                            Set<String> prev = toStates.get(j).apply(s, i, ignoreInInput);
                            for (String r : prev)
                                res.add(out + r);
                        }
                    }
                }
            }

            // Apply epsilon transitions
            int start = firstIndexOf(inSyms, Symbol.EPSILON_CHAR);
            if (start >= 0) {
                for (int j = start; j < inSyms.size() && inSyms.get(j).isEpsilon(); j++) {
                    Symbol out = outSyms.get(j);
                    Set<String> prev = toStates.get(j).apply(s, i, ignoreInInput);
                    for (String r : prev)
                        res.add(out + r);
                }
            }

            // If there is a char left in the string...
            if (!sFin) {
                // ...apply matching literal transitions
                for (Symbol pref : alphabet.getPrefixes(s, i)) {
                    start = firstIndexOf(inSyms, pref);
                    if (start >= 0) {
                        for (int j = start; j < inSyms.size() && inSyms.get(j).equals(pref); j++) {
                            Symbol out = outSyms.get(j);
                            Set<String> prev = toStates.get(j).apply(s, i + inSyms.get(j).length(), ignoreInInput);
                            for (String r : prev)
                                res.add(out + r);
                        }
                    }
                }
                char c = s.charAt(i);
                // ...and identity transitions
                if (!alphabet.contains(c)) {
                    for (MutableState to : idToStates) {
                        Set<String> prev = to.apply(s, i + 1, ignoreInInput);
                        for (String r : prev)
                            res.add(c + r);
                    }
                }
            }

            return res;
        }

        public Set<String> prefixSearch(String prefix, int i, Iterable<String> ignoreInInput, int maxSuffix) {
            // String has been consumed?
            boolean sFin = i >= prefix.length();

            if (sFin)
                maxSuffix--;

            // Result set
            Set<String> res = new HashSet<>();

            // Return empty string if accepting
            if (sFin && accepting)
                res.add("");

            if (!sFin) {
                // Apply ignore transitions
                if (ignoreInInput != null) {
                    for (String ign : ignoreInInput) {
                        int start = firstIndexOf(inSyms, ign);
                        if (start >= 0) {
                            for (int j = start; j < inSyms.size() && inSyms.get(j).equivalentTo(ign); j++) {
                                Set<String> prev = toStates.get(j).prefixSearch(prefix, i, ignoreInInput, maxSuffix);
                                for (String r : prev)
                                    res.add(ign + r);
                            }
                        }
                    }
                }

                // Apply epsilon transitions
                int start = firstIndexOf(inSyms, Symbol.EPSILON_CHAR);
                if (start >= 0) {
                    for (int j = start; j < inSyms.size() && inSyms.get(j).isEpsilon(); j++) {
                        Set<String> prev = toStates.get(j).prefixSearch(prefix, i, ignoreInInput, maxSuffix);
                        res.addAll(prev);
                    }
                }

                // ...apply matching literal transitions
                for (Symbol pref : alphabet.getPrefixes(prefix, i)) {
                    start = firstIndexOf(inSyms, pref);
                    if (start >= 0) {
                        for (int j = start; j < inSyms.size() && inSyms.get(j).equals(pref); j++) {
                            Set<String> prev = toStates.get(j).prefixSearch(prefix, i + inSyms.get(j).length(), ignoreInInput, maxSuffix);
                            for (String r : prev)
                                res.add(pref + r);
                        }
                    }
                }
                char c = prefix.charAt(i);
                // ...and identity transitions
                if (!alphabet.contains(c)) {
                    for (MutableState to : idToStates) {
                        Set<String> prev = to.prefixSearch(prefix, i + 1, ignoreInInput, maxSuffix);
                        for (String r : prev)
                            res.add(c + r);
                    }
                }
            }
            else if (maxSuffix >= 0) {
                for (int s = 0; s < toStates.size(); s++) {
                    Symbol inSym = inSyms.get(s);
                    Set<String> prev = toStates.get(s).prefixSearch(prefix, i + inSym.length(), ignoreInInput, maxSuffix);
                    for (String r : prev)
                        res.add(inSym + r);
                }
            }

            return res;
        }

        private int firstIndexOf(List<Symbol> l, char c) {
            int i = Collections.binarySearch(l, c);
            if (i < 0)
                return -1;
            for (int j = i; j >= 0; j--)
                if (!l.get(j).equivalentTo(c))
                    return j+1;
            return 0;
        }

        private int firstIndexOf(List<Symbol> l, String s) {
            int i = Collections.binarySearch(l, s);
            if (i < 0)
                return -1;
            for (int j = i; j >= 0; j--)
                if (!l.get(j).equivalentTo(s))
                    return j+1;
            return 0;
        }

        private int firstIndexOf(List<Symbol> l, Symbol c) {
            int i = Collections.binarySearch(l, c);
            if (i < 0)
                return -1;
            for (int j = i; j >= 0; j--)
                if (!l.get(j).equals(c))
                    return j+1;
            return 0;
        }

    }


    private static class MutableFSTStateIterator implements FSTStateIterator {

        final MutableFSTOld fst;
        final Alphabet alphabet;

        int s;
        MutableState state;

        int t;
        int i;

        public MutableFSTStateIterator(MutableFSTOld fst) {
            this.fst = fst;
            this.alphabet = new Alphabet(fst.alphabet.getSymbols());
            this.alphabet.addSymbol(Symbol.IDENTITY_STRING);

            s = -1;
            state = fst.start;

            t = -1;
            i = -1;
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
            return fst.start.id;
        }

        @Override
        public int getIdentityId() {
            return alphabet.size()-1;
        }

        @Override
        public boolean hasNextState() {
            return s+1 < fst.states.size();
        }

        @Override
        public void nextState() {
            s++;
            state = fst.states.get(s);
            t = -1;
            i = -1;
        }

        @Override
        public boolean accepting() {
            return state.accepting;
        }

        @Override
        public boolean hasNextTransition() {
            return t+1 < state.toStates.size() || i+1 < state.idToStates.size();
        }

        @Override
        public void nextTransition() {
            if (t+1 < state.toStates.size())
                t++;
            else
                i++;
        }

        @Override
        public boolean identity() {
            return i >= 0;
        }

        @Override
        public int inId() {
            if (identity())
                return -1;
            else
                return state.inSyms.get(t).getId();
        }

        @Override
        public int outId() {
            if (identity())
                return -1;
            else
                return state.outSyms.get(t).getId();
        }

        @Override
        public int toId() {
            if (identity())
                return state.idToStates.get(i).id;
            else
                return state.toStates.get(t).id;
        }
    }

}
