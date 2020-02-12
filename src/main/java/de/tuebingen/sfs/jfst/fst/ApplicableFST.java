package de.tuebingen.sfs.jfst.fst;

import de.tuebingen.sfs.jfst.alphabet.Alphabet;
import de.tuebingen.sfs.jfst.alphabet.Symbol;
import de.tuebingen.sfs.jfst.io.ATTWriter;
import de.tuebingen.sfs.jfst.io.BinaryFSTWriter;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public abstract class ApplicableFST implements FST2 {

    private static final int MAX_SUFFIX = 100;
    private static final int MAX_INSERTIONS = 15;

    public void writeToBinary(OutputStream out) throws IOException {
        BinaryFSTWriter.writeFST(out, this);
    }

    public void writeToATT(File attFile) throws IOException {
        ATTWriter.writeFST(attFile, this);
    }

    abstract int getStartState();

    abstract boolean isAccepting(int stateId);

    abstract Iterator<Transition> getTransitionIterator(int statIdx);

    abstract Iterator<Transition> getTransitionIterator(String s, int statIdx);

    @Override
    public Set<String> apply(String in) {
        return apply(in, null);
    }

    public Set<String> apply(String in, int maxInsertions) {
        return apply(in, maxInsertions, null);
    }

    /**
     * Apply this transducer to an input string.
     * @param in The input string
     * @param ignoreInInput Also take transitions with these input symbols even if they do not occur in the input string
     * @return The output strings matched to the input string by this transducer
     */
    public Set<String> apply(String in, Iterable<String> ignoreInInput) {
        return apply(in, MAX_INSERTIONS, ignoreInInput);
    }

    public Set<String> apply(String in, int maxInsertions, Iterable<String> ignoreInInput) {
        return apply(in, 0, getStartState(), 0, maxInsertions, ignoreInInput);
    }

    private Set<String> apply(String s, int strIdx, int statIdx, int ins, int maxIns, Iterable<String> ignoreInInput) {
        // String has been consumed?
        boolean sFin = strIdx >= s.length();

        // Result set
        Set<String> res = new HashSet<>();

        // Return empty string if accepting
        if (sFin && isAccepting(statIdx))
            res.add("");

        Alphabet alphabet = getAlphabet();

        // Apply ignore transitions
        if (ignoreInInput != null) {
            for (String ign : ignoreInInput) {
                Iterator<Transition> ignIter = getTransitionIterator(ign, statIdx);
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
            Iterator<Transition> epsIter = getTransitionIterator(Symbol.EPSILON_STRING, statIdx);
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
                Iterator<Transition> litIter = getTransitionIterator(pref.asString(), statIdx);
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
                Iterator<Transition> idIter = getTransitionIterator(Symbol.IDENTITY_STRING, statIdx);
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

    private boolean isEpsilon(String s) {
        return s != null && s.length() == 1 && s.charAt(0) == Symbol.EPSILON_CHAR;
    }

    public Set<String> prefixSearch(String prefix) {
        return prefixSearch(prefix, MAX_SUFFIX, null);
    }

    /**
     * Get all input strings in this transducer starting with prefix.
     * @param prefix Prefix of a string
     * @param maxSuffix The maximum number suffix transitions to take (to prevent infinite loop, default is 100)
     * @return The matching input strings in this transducer
     */
    public Set<String> prefixSearch(String prefix, int maxSuffix) {
        return prefixSearch(prefix, maxSuffix, null);
    }

    /**
     * Get all input strings in this transducer starting with prefix.
     * @param prefix Prefix of a string
     * @param ignoreInInput Also take transitions with these input symbols even if they do not occur in the input string
     * @return The matching input strings in this transducer
     */
    public Set<String> prefixSearch(String prefix, Iterable<String> ignoreInInput) {
        return prefixSearch(prefix, MAX_SUFFIX, ignoreInInput);
    }

    /**
     * Get all input strings in this transducer starting with prefix.
     * @param prefix Prefix of a string
     * @param maxSuffix The maximum number suffix transitions to take (to prevent infinite loop, default is 100)
     * @param ignoreInInput Also take transitions with these input symbols even if they do not occur in the input string
     * @return The matching input strings in this transducer
     */
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
        if (sFin && isAccepting(statIdx))
            res.add("");

        Alphabet alphabet = getAlphabet();

        if (!sFin) {
            // Apply ignore transitions
            if (ignoreInInput != null) {
                for (String ign : ignoreInInput) {
                    Iterator<Transition> ignIter = getTransitionIterator(ign, statIdx);
                    while (ignIter.hasNext()) {
                        Transition trans = ignIter.next();
                        Set<String> prev = prefixSearch(s, strIdx, trans.toState, maxSuffix, ignoreInInput);
                        for (String r : prev)
                            res.add(ign + r);
                    }
                }
            }

            // Apply epsilon transitions
            Iterator<Transition> epsIter = getTransitionIterator(Symbol.EPSILON_STRING, statIdx);
            while (epsIter.hasNext()) {
                Transition trans = epsIter.next();
                Set<String> prev = prefixSearch(s, strIdx, trans.toState, maxSuffix, ignoreInInput);
                res.addAll(prev);
            }

            // If there is a char left in the string...
            // ...apply matching literal transitions
            for (Symbol pref : alphabet.getPrefixes(s, strIdx)) {
                Iterator<Transition> litIter = getTransitionIterator(pref.asString(), statIdx);
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
                Iterator<Transition> idIter = getTransitionIterator(Symbol.IDENTITY_STRING, statIdx);
                while (idIter.hasNext()) {
                    Transition trans = idIter.next();
                    Set<String> prev = prefixSearch(s, strIdx + 1, trans.toState, maxSuffix, ignoreInInput);
                    for (String r : prev)
                        res.add(c + r);
                }
            }
        }
        else if (maxSuffix >= 0) {
            Iterator<Transition> allIter = getTransitionIterator(statIdx);
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

    static class Transition {

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
}
