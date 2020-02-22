package de.tuebingen.sfs.jfst.io;

import de.tuebingen.sfs.jfst.transduce.StateIterator;

public interface FileStateIterator extends StateIterator {

    void close();

}
