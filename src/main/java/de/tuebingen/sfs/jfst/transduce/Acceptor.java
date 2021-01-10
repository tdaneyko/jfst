package de.tuebingen.sfs.jfst.transduce;

/**
 * Do not use this class. It currently only has the functionality needed for priority union
 * of transducers.
 */
public abstract class Acceptor implements Automaton {

    public abstract Transducer asTransducer();

    public abstract void complement();

    public abstract void compose(Transducer other);

}
