package de.tuebingen.sfs.jfst.fst;

public class CompactTransition implements Comparable<Object> {

    // Mask to get the input symbol out of a transition
    static final long getInSym = 0xffff000000000000L;
    // Mask to get the output symbol out of a transition
    static final long getOutSym = 0x0000ffff00000000L;
    // Mask to get the to-state out of a transition
    static final long getToState = 0x00000000ffffffffL;

    private long transition;

    public CompactTransition(int inSym, int outSym, int toId) {
        this.transition = makeTransition(inSym, outSym, toId);
    }

    public int getInSym() {
        return inIdFromTransition(transition);
    }

    public void setInSym(int inSym) {
        transition = (transition & ~(getInSym)) | (((long) inSym) << 48);
    }

    public int getOutSym() {
        return outIdFromTransition(transition);
    }

    public void setOutSym(int outSym) {
        transition = (transition & ~(getOutSym)) | (((long) outSym) << 32);
    }

    public int getToState() {
        return toIdFromTransition(transition);
    }

    public void setToState(int toState) {
        transition = (transition & ~(getToState)) | ((long) toState);
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


    public static long makeTransition(int inSym, int outSym, int toId) {
        return makeTransition((long) inSym, outSym, toId);
    }

    public static long makeTransition(long inSym, long outSym, long toId) {
        return (((inSym << 16) | outSym) << 32) | toId;
    }

    public static int inIdFromTransition(long transition) {
        return (int) ((transition & getInSym) >> 48);
    }

    public static int outIdFromTransition(long transition) {
        return (int) ((transition & getOutSym) >> 32);
    }

    public static int toIdFromTransition(long transition) {
        return (int) (transition & getToState);
    }

    public static boolean isIdentityTransition(long transition, int idIdx) {
        return inIdFromTransition(transition) == idIdx;
    }
}