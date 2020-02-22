package de.tuebingen.sfs.jfst.transduce;

public abstract class MutableTransducer extends ApplicableTransducer {

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

    public abstract void invert();

    public abstract void reverse();

    public abstract void concat(Transducer other);

    public abstract void union(Transducer other);

    public abstract void priorityUnion(Transducer other);

    public abstract void intersect(Transducer other);

    public abstract void subtract(Transducer other);

    public abstract void compose(Transducer other);


}
