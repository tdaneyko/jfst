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
                new File(TEST_DIR + "testMutable1.att")), FstProducer.HFST);

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
                new File(TEST_DIR + "testMutable1.att")), FstProducer.HFST);

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

    public void testRepeat() throws IOException {
        MutableCompactTransducer fst = MutableCompactTransducer.readFromATT(new FileInputStream(
                new File(TEST_DIR + "testMutable1.att")), FstProducer.HFST);

        assertEquals(Collections.singleton("BDG"), fst.apply("bdg"));
        assertEquals(Collections.singleton("XXXEFG"), fst.apply("xxxefg"));
        assertEquals(Collections.emptySet(), fst.apply("bg"));

        fst.repeat(3);

        assertEquals(Collections.emptySet(), fst.apply("bdg"));
        assertEquals(Collections.emptySet(), fst.apply("bdgbdg"));
        assertEquals(Collections.singleton("BDGBDGBDG"), fst.apply("bdgbdgbdg"));
        assertEquals(Collections.emptySet(), fst.apply("bdgbdgbdgbdg"));
    }

    public void testRepeatMin() throws IOException {
        MutableCompactTransducer fst = MutableCompactTransducer.readFromATT(new FileInputStream(
                new File(TEST_DIR + "testMutable1.att")), FstProducer.HFST);

        assertEquals(Collections.singleton("BDG"), fst.apply("bdg"));
        assertEquals(Collections.singleton("XXXEFG"), fst.apply("xxxefg"));
        assertEquals(Collections.emptySet(), fst.apply("bg"));

        fst.repeatMin(3);

        assertEquals(Collections.emptySet(), fst.apply(""));
        assertEquals(Collections.emptySet(), fst.apply("bdg"));
        assertEquals(Collections.emptySet(), fst.apply("bdgbdg"));
        assertEquals(Collections.singleton("BDGBDGBDG"), fst.apply("bdgbdgbdg"));
        assertEquals(Collections.singleton("BDGBDGBDGBDG"), fst.apply("bdgbdgbdgbdg"));
    }

    public void testKleeneStar() throws IOException {
        MutableCompactTransducer fst = MutableCompactTransducer.readFromATT(new FileInputStream(
                new File(TEST_DIR + "testMutable1.att")), FstProducer.HFST);

        assertEquals(Collections.singleton("BDG"), fst.apply("bdg"));
        assertEquals(Collections.singleton("XXXEFG"), fst.apply("xxxefg"));
        assertEquals(Collections.emptySet(), fst.apply("bg"));

        fst.kleeneStar();

        assertEquals(Collections.singleton(""), fst.apply(""));
        assertEquals(Collections.singleton("BDG"), fst.apply("bdg"));
        assertEquals(Collections.singleton("BDGBDGBDGBDG"), fst.apply("bdgbdgbdgbdg"));
    }

    public void testInverse() throws IOException {
        MutableCompactTransducer fst = MutableCompactTransducer.readFromATT(new FileInputStream(
                new File(TEST_DIR + "testMutable1.att")), FstProducer.HFST);

        assertEquals(Collections.singleton("BDG"), fst.apply("bdg"));
        assertEquals(Collections.singleton("XXXEFG"), fst.apply("xxxefg"));
        assertEquals(Collections.emptySet(), fst.apply("bg"));

        fst.inverse();

        assertEquals(Collections.singleton("bdg"), fst.apply("BDG"));
        assertEquals(Collections.singleton("xxxefg"), fst.apply("XXXEFG"));
    }

    public void testReverse() throws IOException {
        MutableCompactTransducer fst = MutableCompactTransducer.readFromATT(new FileInputStream(
                new File(TEST_DIR + "testMutable1.att")), FstProducer.HFST);

        assertEquals(Collections.singleton("BDG"), fst.apply("bdg"));
        assertEquals(Collections.singleton("XXXEFG"), fst.apply("xxxefg"));
        assertEquals(Collections.emptySet(), fst.apply("bg"));

        fst.reverse();

        assertEquals(Collections.singleton("GDB"), fst.apply("gdb"));
        assertEquals(Collections.singleton("GFEXXX"), fst.apply("gfexxx"));
        assertEquals(Collections.emptySet(), fst.apply("gb"));
    }

    public void testConcatSameAlph() throws IOException {
        MutableCompactTransducer fst1 = MutableCompactTransducer.readFromATT(new FileInputStream(
                new File(TEST_DIR + "testMutable1.att")), FstProducer.HFST);
        MutableCompactTransducer fst2 = MutableCompactTransducer.readFromATT(new FileInputStream(
                new File(TEST_DIR + "testMutable1.att")), FstProducer.HFST, true);

        assertEquals(Collections.singleton("BDG"), fst1.apply("bdg"));
        assertEquals(Collections.singleton("XXXEFG"), fst1.apply("xxxefg"));
        assertEquals(Collections.emptySet(), fst1.apply("bg"));

        fst1.concat(fst2);

        assertEquals(Collections.emptySet(), fst1.apply("bdg"));
        assertEquals(Collections.singleton("BDGbdg"), fst1.apply("bdgBDG"));
        assertEquals(Collections.singleton("XXXEFGbdg"), fst1.apply("xxxefgBDG"));
        assertEquals(Collections.emptySet(), fst1.apply("bg"));
    }

    public void testConcatDiffAlph() throws IOException {
        MutableCompactTransducer fst1 = MutableCompactTransducer.readFromATT(new FileInputStream(
                new File(TEST_DIR + "testMutable1.att")), FstProducer.HFST);
        MutableCompactTransducer fst2 = MutableCompactTransducer.readFromATT(new FileInputStream(
                new File(TEST_DIR + "testMutable2.att")), FstProducer.HFST, true);

        assertEquals(Collections.singleton("BDG"), fst1.apply("bdg"));
        assertEquals(Collections.singleton("XXXEFG"), fst1.apply("xxxefg"));
        assertEquals(Collections.emptySet(), fst1.apply("bg"));

        fst1.concat(fst2);

        assertEquals(Collections.emptySet(), fst1.apply("bdg"));
        assertEquals(Collections.singleton("BDGбdг"), fst1.apply("bdgБDГ"));
        assertEquals(Collections.singleton("XXXEFGбdг"), fst1.apply("xxxefgБDГ"));
        assertEquals(Collections.emptySet(), fst1.apply("bg"));
    }

    public void testUnion() throws IOException {
        MutableCompactTransducer fst1 = MutableCompactTransducer.readFromATT(new FileInputStream(
                new File(TEST_DIR + "testMutable1.att")), FstProducer.HFST);
        MutableCompactTransducer fst2 = MutableCompactTransducer.readFromATT(new FileInputStream(
                new File(TEST_DIR + "testMutable2.att")), FstProducer.HFST);

        assertEquals(Collections.singleton("BDG"), fst1.apply("bdg"));
        assertEquals(Collections.singleton("XXXEFG"), fst1.apply("xxxefg"));
        assertEquals(Collections.emptySet(), fst1.apply("bg"));

        fst1.union(fst2);

        assertEquals(Collections.singleton("BDG"), fst1.apply("bdg"));
        assertEquals(Collections.singleton("БDГ"), fst1.apply("бdг"));
        assertEquals(Collections.emptySet(), fst1.apply("бdg"));
        assertEquals(Collections.emptySet(), fst1.apply("bg"));
    }

}
