package de.tuebingen.sfs.jfst.transduce;

import de.tuebingen.sfs.jfst.symbol.Alphabet;

public abstract class MutableTransducer extends ApplicableTransducer {

    @Override
    public MutableTransducer getMutableCopy() {
        return this;
    }

    public abstract void addSymbol(String symbol);

    public int addState() {
        return addState(false);
    }

    public abstract int addState(boolean accepting);

    public abstract void setAccepting(int stateId, boolean accepting);

    public abstract void addTransition(int from, String inSym, String outSym, int to);

    public void addEpsilonTransition(int from, int to) {
        addTransition(from, Alphabet.EPSILON_STRING, Alphabet.EPSILON_STRING, to);
    }

    public abstract void determinize();

    public abstract void minimize();

    public abstract void repeat(int n);

    public abstract void repeatMin(int n);

    public void kleeneStar() {
        repeatMin(0);
    }

    public void kleenePlus() {
        repeatMin(1);
    }

    public abstract void optional();

    public abstract void inverse();

    public abstract void reverse();

    public abstract void concat(Transducer other);

    public abstract void union(Transducer other);

    public abstract void priorityUnion(Transducer other);

    public abstract void intersect(Transducer other);

    public abstract void subtract(Transducer other);

    public abstract void compose(Transducer other);


}
