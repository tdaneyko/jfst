package de.tuebingen.sfs.jfst.io;

import de.tuebingen.sfs.jfst.fst.FSTStateIterator;

public interface FSTFileStateIterator extends FSTStateIterator {

    void close();

}
