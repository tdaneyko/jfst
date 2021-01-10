package de.tuebingen.sfs.jfst.transduce.compact;

import de.tuebingen.sfs.jfst.symbol.Alphabet;
import de.tuebingen.sfs.jfst.transduce.Acceptor;
import de.tuebingen.sfs.jfst.transduce.StateIterator;
import de.tuebingen.sfs.jfst.transduce.Transducer;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Do not use this class. It currently only has the functionality needed for priority union
 * of transducers.
 */
public class CompactAcceptor extends Acceptor {

    public enum Projection { LOWER, UPPER }

    public CompactAcceptor projectTransducer(StateIterator transducer, Projection direction) {
        return new CompactAcceptor(transducer, direction);
    }

    private CompactAcceptor(StateIterator transducer, Projection direction) {
        // UP: trans.setOutSym(trans.getInSym());
    }

    @Override
    public void writeToBinary(OutputStream out) throws IOException {
        asTransducer().writeToBinary(out);
    }

    @Override
    public void writeToATT(File attFile) throws IOException {
        asTransducer().writeToATT(attFile);
    }

    @Override
    public int nOfStates() {
        return 0;
    }

    @Override
    public int nOfTransitions() {
        return 0;
    }

    @Override
    public Alphabet getAlphabet() {
        return null;
    }

    @Override
    public StateIterator iter() {
        return null;
    }

    @Override
    public Transducer asTransducer() {
        return new CompactTransducer(iter());
    }

    @Override
    public void complement() {

    }

    @Override
    public void compose(Transducer other) {

    }
}
