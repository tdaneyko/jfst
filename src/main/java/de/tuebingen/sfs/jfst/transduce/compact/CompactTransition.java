package de.tuebingen.sfs.jfst.transduce.compact;

import de.tuebingen.sfs.jfst.symbol.Alphabet;

public class CompactTransition implements Comparable<Object> {

    // Mask to get the input symbol out of a transition
    public static final long GET_IN_SYM = 0xffff000000000000L;
    // Mask to get the output symbol out of a transition
    public static final long GET_OUT_SYM = 0x0000ffff00000000L;
    // Mask to get the to-state out of a transition
    public static final long GET_TO_STATE = 0x00000000ffffffffL;

    private long transition;

    public CompactTransition(int inSym, int outSym, int toId) {
        this.transition = makeTransition(inSym, outSym, toId);
    }

    public CompactTransition(long transition) {
        this.transition = transition;
    }

    public int getInSym() {
        return inIdFromTransition(transition);
    }

    void setInSym(int inSym) {
        transition = (transition & ~(GET_IN_SYM)) | (((long) inSym) << 48);
    }

    public int getOutSym() {
        return outIdFromTransition(transition);
    }

    void setOutSym(int outSym) {
        transition = (transition & ~(GET_OUT_SYM)) | (((long) outSym) << 32);
    }

    void invert() {
        int oldInSym = getInSym();
        setInSym(getOutSym());
        setOutSym(oldInSym);
    }

    public int getSymPair() {
        return (int) ((transition >> 32) & GET_TO_STATE);
    }

    public int getToState() {
        return toIdFromTransition(transition);
    }

    void setToState(int toState) {
        transition = (transition & ~(GET_TO_STATE)) | ((long) toState);
    }

    public boolean epsilon(int epsIdx) {
        return isEpsilonTransition(transition, epsIdx);
    }

    public boolean identity(int idIdx) {
        return isIdentityTransition(transition, idIdx);
    }

    public long getInternalRepresentation() {
        return transition;
    }

    @Override
    public int compareTo(Object other) {
        if (other instanceof CompactTransition)
            return Long.compare(this.transition, ((CompactTransition) other).transition);
        if (other instanceof Long)
            return Long.compare(this.transition, (Long) other);
        return -1;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CompactTransition)
            return this.transition == ((CompactTransition) obj).transition;
        return false;
    }

    @Override
    public String toString() {
        return getInSym() + ":" + getOutSym() + " -> " + getToState();
    }

    public String toString(Alphabet alphabet) {
        return alphabet.getSymbol(getInSym()) + ":" + alphabet.getSymbol(getOutSym())
                + " -> " + getToState();
    }

    public static long makeTransition(int inSym, int outSym, int toId) {
        return makeTransition((long) inSym, outSym, toId);
    }

    public static long makeTransition(long inSym, long outSym, long toId) {
        return (((inSym << 16) | outSym) << 32) | toId;
    }

    public static int inIdFromTransition(long transition) {
        return (int) ((transition & GET_IN_SYM) >> 48);
    }

    public static int outIdFromTransition(long transition) {
        return (int) ((transition & GET_OUT_SYM) >> 32);
    }

    public static int toIdFromTransition(long transition) {
        return (int) (transition & GET_TO_STATE);
    }

    public static boolean isIdentityTransition(long transition, int idIdx) {
        return inIdFromTransition(transition) == idIdx;
    }

    public static boolean isEpsilonTransition(long transition, int epsIdx) {
        return inIdFromTransition(transition) == epsIdx && outIdFromTransition(transition) == epsIdx;
    }
}
