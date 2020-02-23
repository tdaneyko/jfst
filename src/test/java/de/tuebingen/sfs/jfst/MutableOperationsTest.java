package de.tuebingen.sfs.jfst;

import de.tuebingen.sfs.jfst.io.FstProducer;
import de.tuebingen.sfs.jfst.transduce.compact.MutableCompactTransducer;
import junit.framework.TestCase;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;

public class MutableOperationsTest extends TestCase {

    private static final String TEST_DIR = "src/test/resources/";

    public void testRemoveEpsilons() throws IOException {
        MutableCompactTransducer fst = MutableCompactTransducer.readFromATT(new FileInputStream(
                new File(TEST_DIR + "testDeterminize.att")), FstProducer.HFST);

        assertEquals(10, fst.nOfStates());
        assertEquals(14, fst.nOfTransitions());
        assertEquals(Collections.singleton("BDG"), fst.apply("bdg"));
        assertEquals(Collections.singleton("XXXEFG"), fst.apply("xxxefg"));
        assertEquals(Collections.emptySet(), fst.apply("bg"));

        int statesPrev = fst.nOfStates();
        fst.removeEpsilons();
        fst.writeToATT(new File(TEST_DIR + "testEpsilonsOut.att"));

        assertEquals(statesPrev, fst.nOfStates());
        assertEquals(18, fst.nOfTransitions());
        assertEquals(Collections.singleton("BDG"), fst.apply("bdg"));
        assertEquals(Collections.singleton("XXXEFG"), fst.apply("xxxefg"));
        assertEquals(Collections.emptySet(), fst.apply("bg"));
    }

    public void testDeterminize() throws IOException {
        MutableCompactTransducer fst = MutableCompactTransducer.readFromATT(new FileInputStream(
                new File(TEST_DIR + "testDeterminize.att")), FstProducer.HFST);

        assertEquals(10, fst.nOfStates());
        assertEquals(14, fst.nOfTransitions());
        assertEquals(Collections.singleton("BDG"), fst.apply("bdg"));
        assertEquals(Collections.singleton("XXXEFG"), fst.apply("xxxefg"));
        assertEquals(Collections.emptySet(), fst.apply("bg"));

        fst.determinize();
        fst.writeToATT(new File(TEST_DIR + "testDeterminizeOut.att"));

        assertEquals(10, fst.nOfStates());
        assertEquals(13, fst.nOfTransitions());
        assertEquals(Collections.singleton("BDG"), fst.apply("bdg"));
        assertEquals(Collections.singleton("XXXEFG"), fst.apply("xxxefg"));
        assertEquals(Collections.emptySet(), fst.apply("bg"));
    }

}
