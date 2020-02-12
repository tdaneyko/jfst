package de.tuebingen.sfs.jfst.fst;

public abstract class MutableFST extends FST {

    public abstract void determinize();

    public abstract void minimize();

    public abstract void repeat(int min, int max);

    public void repeatMin(int n) {
        repeat(n, Integer.MAX_VALUE);
    }

    public void repeatTimes(int n) {
        repeat(n, n);
    }

    public void kleeneStar() {
        repeatMin(0);
    }

    public void kleenePlus() {
        repeatMin(1);
    }

    public abstract void optional();

    public abstract void invert();

    public abstract void reverse();

    public abstract void concatenate(FST other);

    public abstract void disjunct(FST other);

    public abstract void priorityUnion(FST other);

    public abstract void compose(FST other);

    public abstract void subtract(FST other);


}
