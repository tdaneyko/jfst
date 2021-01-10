package de.tuebingen.sfs.jfst;

import de.tuebingen.sfs.jfst.io.FstProducer;
import de.tuebingen.sfs.jfst.transduce.MutableTransducer;
import de.tuebingen.sfs.jfst.transduce.compact.MutableCompactTransducer;
import junit.framework.TestCase;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

public class MutableOperationsTest extends TestCase {

    private static final String TEST_DIR = "src/test/resources/";

    public void testAddSymbol() throws IOException {
        MutableCompactTransducer fst = MutableCompactTransducer.readFromATT(new FileInputStream(
                new File(TEST_DIR + "testMutable3.att")), FstProducer.HFST_ATT);

        assertEquals(2, fst.nOfStates());
        assertEquals(2, fst.nOfTransitions());
        assertEquals(Collections.singleton("a"), fst.apply("a"));
        assertEquals(Collections.emptySet(), fst.apply("b"));

        fst.addSymbol("b");
        fst.writeToATT(new File(TEST_DIR + "testAddSymbolOut.att"));

        assertEquals(2, fst.nOfStates());
        assertEquals(3, fst.nOfTransitions());
        assertEquals(new HashSet<>(Arrays.asList("a", "b")), fst.apply("a"));
        assertEquals(Collections.emptySet(), fst.apply("b"));
    }

    public void testCopy() throws IOException {
        MutableCompactTransducer fst1 = MutableCompactTransducer.readFromATT(new FileInputStream(
                new File(TEST_DIR + "testMutable1.att")), FstProducer.HFST_ATT);

        assertEquals(10, fst1.nOfStates());
        assertEquals(14, fst1.nOfTransitions());
        assertEquals(Collections.singleton("BDG"), fst1.apply("bdg"));
        assertEquals(Collections.singleton("XXXEFG"), fst1.apply("xxxefg"));
        assertEquals(Collections.emptySet(), fst1.apply("bg"));

        MutableCompactTransducer fst2 = fst1.copy();
        fst2.writeToATT(new File(TEST_DIR + "testCopyOut.att"));

        assertEquals(fst1.nOfStates(), fst2.nOfStates());
        assertEquals(fst1.nOfTransitions(), fst2.nOfTransitions());
        assertEquals(fst1.apply("bdg"), fst2.apply("bdg"));
        assertEquals(fst1.apply("xxxefg"), fst2.apply("xxxefg"));
        assertEquals(fst1.apply("bg"), fst2.apply("bg"));
    }

    public void testRemoveEpsilons() throws IOException {
        MutableCompactTransducer fst = MutableCompactTransducer.readFromATT(new FileInputStream(
                new File(TEST_DIR + "testMutable1.att")), FstProducer.HFST_ATT);

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
                new File(TEST_DIR + "testMutable1.att")), FstProducer.HFST_ATT);

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

    public void testRemoveUnreachableStates() throws IOException {
        MutableCompactTransducer fst = MutableCompactTransducer.readFromATT(new FileInputStream(
                new File(TEST_DIR + "testMutable1.att")), FstProducer.HFST_ATT);

        int s = fst.addState();
        fst.addTransition(s, "b", "B", 0);

        assertEquals(11, fst.nOfStates());
        assertEquals(15, fst.nOfTransitions());
        assertEquals(Collections.singleton("BDG"), fst.apply("bdg"));
        assertEquals(Collections.singleton("XXXEFG"), fst.apply("xxxefg"));
        assertEquals(Collections.emptySet(), fst.apply("bg"));

        fst.removeUnreachableStates();

        assertEquals(10, fst.nOfStates());
        assertEquals(14, fst.nOfTransitions());
        assertEquals(Collections.singleton("BDG"), fst.apply("bdg"));
        assertEquals(Collections.singleton("XXXEFG"), fst.apply("xxxefg"));
        assertEquals(Collections.emptySet(), fst.apply("bg"));
    }

    public void testRemoveTraps() throws IOException {
        MutableCompactTransducer fst = MutableCompactTransducer.readFromATT(new FileInputStream(
                new File(TEST_DIR + "testMutable1.att")), FstProducer.HFST_ATT);

        int s = fst.addState();
        fst.addTransition(0, "b", "B", s);

        assertEquals(11, fst.nOfStates());
        assertEquals(15, fst.nOfTransitions());
        assertEquals(Collections.singleton("BDG"), fst.apply("bdg"));
        assertEquals(Collections.singleton("XXXEFG"), fst.apply("xxxefg"));
        assertEquals(Collections.emptySet(), fst.apply("bg"));

        fst.removeTraps();
        fst.writeToATT(new File(TEST_DIR + "testRemoveTrapsOut.att"));

        assertEquals(10, fst.nOfStates());
        assertEquals(14, fst.nOfTransitions());
        assertEquals(Collections.singleton("BDG"), fst.apply("bdg"));
        assertEquals(Collections.singleton("XXXEFG"), fst.apply("xxxefg"));
        assertEquals(Collections.emptySet(), fst.apply("bg"));
    }

    public void testMinimize() throws IOException {
        MutableCompactTransducer fst = MutableCompactTransducer.readFromATT(new FileInputStream(
                new File(TEST_DIR + "testMutable1.att")), FstProducer.HFST_ATT);

        assertEquals(10, fst.nOfStates());
        assertEquals(14, fst.nOfTransitions());
        assertEquals(Collections.singleton("BDG"), fst.apply("bdg"));
        assertEquals(Collections.singleton("XXXEFG"), fst.apply("xxxefg"));
        assertEquals(Collections.emptySet(), fst.apply("bg"));

        fst.minimize();
        fst.writeToATT(new File(TEST_DIR + "testMinimizeOut.att"));

        assertEquals(8, fst.nOfStates());
        assertEquals(13, fst.nOfTransitions());
        assertEquals(Collections.singleton("BDG"), fst.apply("bdg"));
        assertEquals(Collections.singleton("XXXEFG"), fst.apply("xxxefg"));
        assertEquals(Collections.emptySet(), fst.apply("bg"));
    }

    public void testRepeat() throws IOException {
        MutableCompactTransducer fst = MutableCompactTransducer.readFromATT(new FileInputStream(
                new File(TEST_DIR + "testMutable1.att")), FstProducer.HFST_ATT);

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
                new File(TEST_DIR + "testMutable1.att")), FstProducer.HFST_ATT);

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
                new File(TEST_DIR + "testMutable1.att")), FstProducer.HFST_ATT);

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
                new File(TEST_DIR + "testMutable1.att")), FstProducer.HFST_ATT);

        assertEquals(Collections.singleton("BDG"), fst.apply("bdg"));
        assertEquals(Collections.singleton("XXXEFG"), fst.apply("xxxefg"));
        assertEquals(Collections.emptySet(), fst.apply("bg"));

        fst.inverse();

        assertEquals(Collections.singleton("bdg"), fst.apply("BDG"));
        assertEquals(Collections.singleton("xxxefg"), fst.apply("XXXEFG"));
    }

    public void testReverse() throws IOException {
        MutableCompactTransducer fst = MutableCompactTransducer.readFromATT(new FileInputStream(
                new File(TEST_DIR + "testMutable1.att")), FstProducer.HFST_ATT);

        assertEquals(Collections.singleton("BDG"), fst.apply("bdg"));
        assertEquals(Collections.singleton("XXXEFG"), fst.apply("xxxefg"));
        assertEquals(Collections.emptySet(), fst.apply("bg"));

        fst.reverse();
        fst.writeToATT(new File(TEST_DIR + "testReverseOut.att"));

        assertEquals(Collections.singleton("GDB"), fst.apply("gdb"));
        assertEquals(Collections.singleton("GFEXXX"), fst.apply("gfexxx"));
        assertEquals(Collections.emptySet(), fst.apply("gb"));
    }

//    public void testComplement() throws IOException {
//        MutableCompactTransducer fst = MutableCompactTransducer.readFromATT(new FileInputStream(
//                new File(TEST_DIR + "testMutable1.att")), FstProducer.HFST);
//
//        assertEquals(Collections.singleton("BDG"), fst.apply("bdg"));
//        assertEquals(Collections.singleton("XXXEFG"), fst.apply("xxxefg"));
//        assertEquals(Collections.emptySet(), fst.apply("bg"));
//
//        fst.complement();
//        fst.writeToATT(new File(TEST_DIR + "testComplementOut.att"));
//
//        assertEquals(Collections.emptySet(), fst.apply("bdg"));
//        assertEquals(Collections.emptySet(), fst.apply("xxxefg"));
//        assertEquals(Collections.emptySet(), fst.apply("bg"));
//    }

    public void testConcatSameAlph() throws IOException {
        MutableCompactTransducer fst1 = MutableCompactTransducer.readFromATT(new FileInputStream(
                new File(TEST_DIR + "testMutable1.att")), FstProducer.HFST_ATT);
        MutableCompactTransducer fst2 = MutableCompactTransducer.readFromATT(new FileInputStream(
                new File(TEST_DIR + "testMutable1.att")), FstProducer.HFST_ATT, true);

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
                new File(TEST_DIR + "testMutable1.att")), FstProducer.HFST_ATT);
        MutableCompactTransducer fst2 = MutableCompactTransducer.readFromATT(new FileInputStream(
                new File(TEST_DIR + "testMutable2.att")), FstProducer.HFST_ATT, true);

        assertEquals(Collections.singleton("BDG"), fst1.apply("bdg"));
        assertEquals(Collections.singleton("XXXEFG"), fst1.apply("xxxefg"));
        assertEquals(Collections.emptySet(), fst1.apply("bg"));

        fst1.concat(fst2);

        assertEquals(Collections.emptySet(), fst1.apply("bdg"));
        assertEquals(Collections.singleton("BDGбdг"), fst1.apply("bdgБDГ"));
        assertEquals(Collections.singleton("XXXEFGбdг"), fst1.apply("xxxefgБDГ"));
        assertEquals(Collections.emptySet(), fst1.apply("bg"));
    }

    public void testProjectUp() throws IOException {
        MutableCompactTransducer fst9 = MutableCompactTransducer.readFromATT(new FileInputStream(
                new File(TEST_DIR + "testMutable9.att")), FstProducer.HFST_ATT);

        assertEquals(Collections.singleton("AC"), fst9.apply("ac"));
        assertEquals(Collections.singleton("AX"), fst9.apply("ax"));

        fst9.projectUp();
        fst9.writeToATT(new File(TEST_DIR + "testProjectUp.att"));

        assertEquals(Collections.singleton("ac"), fst9.apply("ac"));
        assertEquals(Collections.singleton("ax"), fst9.apply("ax"));
    }

    public void testUnion() throws IOException {
        MutableCompactTransducer fst1 = MutableCompactTransducer.readFromATT(new FileInputStream(
                new File(TEST_DIR + "testMutable1.att")), FstProducer.HFST_ATT);
        MutableCompactTransducer fst2 = MutableCompactTransducer.readFromATT(new FileInputStream(
                new File(TEST_DIR + "testMutable2.att")), FstProducer.HFST_ATT);

        assertEquals(Collections.singleton("BDG"), fst1.apply("bdg"));
        assertEquals(Collections.singleton("XXXEFG"), fst1.apply("xxxefg"));
        assertEquals(Collections.emptySet(), fst1.apply("bg"));

        fst1.union(fst2);

        assertEquals(Collections.singleton("BDG"), fst1.apply("bdg"));
        assertEquals(Collections.singleton("БDГ"), fst1.apply("бdг"));
        assertEquals(Collections.emptySet(), fst1.apply("бdg"));
        assertEquals(Collections.emptySet(), fst1.apply("bg"));
    }

    public void testIntersection() throws IOException {
        MutableCompactTransducer fst1 = MutableCompactTransducer.readFromATT(new FileInputStream(
                new File(TEST_DIR + "testMutable1.att")), FstProducer.HFST_ATT);
        MutableCompactTransducer fst2 = MutableCompactTransducer.readFromATT(new FileInputStream(
                new File(TEST_DIR + "testMutable2.att")), FstProducer.HFST_ATT);

        assertEquals(Collections.singleton("BDG"), fst1.apply("bdg"));
        assertEquals(Collections.singleton("XXXEFG"), fst1.apply("xxxefg"));
        assertEquals(Collections.emptySet(), fst1.apply("bg"));

        fst1.intersect(fst2);
        fst1.writeToATT(new File(TEST_DIR + "testIntersectOut.att"));

        assertEquals(Collections.singleton("AD"), fst1.apply("ad"));
        assertEquals(Collections.singleton("C"), fst1.apply("c"));
        assertEquals(Collections.emptySet(), fst1.apply("bdg"));
        assertEquals(Collections.emptySet(), fst1.apply("бdг"));
    }

    public void testSubtract() throws IOException {
        // Test pair 1
        MutableCompactTransducer fst1 = MutableCompactTransducer.readFromATT(new FileInputStream(
                new File(TEST_DIR + "testMutable1.att")), FstProducer.HFST_ATT);
        MutableCompactTransducer fst2 = MutableCompactTransducer.readFromATT(new FileInputStream(
                new File(TEST_DIR + "testMutable2.att")), FstProducer.HFST_ATT);

        assertEquals(Collections.singleton("BDG"), fst1.apply("bdg"));
        assertEquals(Collections.emptySet(), fst2.apply("bdg"));
        assertEquals(Collections.singleton("AD"), fst1.apply("ad"));
        assertEquals(fst1.apply("ad"), fst2.apply("ad"));
        assertEquals(Collections.singleton("C"), fst1.apply("c"));
        assertEquals(fst1.apply("c"), fst2.apply("c"));

        fst2.complement();
        fst2.writeToATT(new File(TEST_DIR + "testComplementOut.att"));
        fst1.intersect(fst2);
        fst1.writeToATT(new File(TEST_DIR + "testSubtractOut.att"));

        assertEquals(Collections.singleton("BDG"), fst1.apply("bdg"));
        assertEquals(Collections.emptySet(), fst1.apply("ad"));
        assertEquals(Collections.emptySet(), fst1.apply("c"));

        // Test pair 2
        MutableCompactTransducer fst4 = MutableCompactTransducer.readFromATT(new FileInputStream(
                new File(TEST_DIR + "testMutable4.att")), FstProducer.HFST_ATT);
        MutableCompactTransducer fst5 = MutableCompactTransducer.readFromATT(new FileInputStream(
                new File(TEST_DIR + "testMutable5.att")), FstProducer.HFST_ATT);

        assertEquals(Collections.singleton("b"), fst4.apply("a"));
        assertEquals(Collections.singleton("d"), fst4.apply("c"));

        fst5.complement();
        fst5.writeToATT(new File(TEST_DIR + "testComplementOut2.att"));
        fst4.intersect(fst5);
        fst4.writeToATT(new File(TEST_DIR + "testSubtractOut2.att"));

        assertEquals(3, fst4.nOfStates());
        assertEquals(2, fst4.nOfTransitions());
        assertEquals(Collections.singleton("b"), fst4.apply("a"));
        assertEquals(Collections.emptySet(), fst4.apply("c"));

        // Test pair 3
        MutableCompactTransducer fst6 = MutableCompactTransducer.readFromATT(new FileInputStream(
                new File(TEST_DIR + "testMutable6.att")), FstProducer.HFST_ATT);
        MutableCompactTransducer fst7 = MutableCompactTransducer.readFromATT(new FileInputStream(
                new File(TEST_DIR + "testMutable7.att")), FstProducer.HFST_ATT);

        assertEquals(Collections.singleton("ac"), fst6.apply("ac"));
        assertEquals(Collections.singleton("ac"), fst7.apply("ac"));
        assertEquals(Collections.singleton("abc"), fst6.apply("abc"));
        assertEquals(Collections.emptySet(), fst7.apply("abc"));
        assertEquals(Collections.singleton("de"), fst6.apply("de"));
        assertEquals(Collections.singleton("dh"), fst7.apply("dh"));

        fst6.subtract(fst7);
        fst6.writeToATT(new File(TEST_DIR + "testSubtractOut3.att"));

        assertEquals(Collections.emptySet(), fst6.apply("ac"));
        assertEquals(Collections.singleton("abc"), fst6.apply("abc"));
        assertEquals(Collections.singleton("de"), fst6.apply("de"));
    }

    public void testPriorityUnion() throws IOException {
        MutableCompactTransducer fst7 = MutableCompactTransducer.readFromATT(new FileInputStream(
                new File(TEST_DIR + "testMutable7.att")), FstProducer.HFST_ATT);
        MutableCompactTransducer fst9 = MutableCompactTransducer.readFromATT(new FileInputStream(
                new File(TEST_DIR + "testMutable9.att")), FstProducer.HFST_ATT);

        assertEquals(Collections.singleton("ac"), fst7.apply("ac"));
        assertEquals(Collections.emptySet(), fst7.apply("ax"));
        assertEquals(Collections.singleton("dh"), fst7.apply("dh"));
        assertEquals(Collections.singleton("dfg"), fst7.apply("dfg"));

        assertEquals(Collections.singleton("AC"), fst9.apply("ac"));
        assertEquals(Collections.singleton("AX"), fst9.apply("ax"));
        assertEquals(Collections.emptySet(), fst9.apply("dh"));
        assertEquals(Collections.emptySet(), fst9.apply("dfg"));

//        fst7.priorityUnion(fst9);
//        fst7.writeToATT(new File(TEST_DIR + "testPriorityUnionOut.att"));
        MutableCompactTransducer upper = fst7.copy();
        upper.projectUp();
        upper.writeToATT(new File(TEST_DIR + "testPriorityUnionOut1.att"));
        upper.complement();
        upper.writeToATT(new File(TEST_DIR + "testPriorityUnionOut2.att"));
        upper.compose(fst9);
        upper.writeToATT(new File(TEST_DIR + "testPriorityUnionOut3.att"));
        upper.minimize();
        upper.writeToATT(new File(TEST_DIR + "testPriorityUnionOut.att"));

        assertEquals(Collections.singleton("ac"), fst7.apply("ac"));
        assertEquals(Collections.singleton("AX"), fst7.apply("ax"));
        assertEquals(Collections.singleton("dh"), fst7.apply("dh"));
        assertEquals(Collections.singleton("dfg"), fst7.apply("dfg"));
    }

    public void testComposition() throws IOException {
        MutableCompactTransducer fst1 = MutableCompactTransducer.readFromATT(new FileInputStream(
                new File(TEST_DIR + "testMutable1.att")), FstProducer.HFST_ATT);
        MutableCompactTransducer fst8 = MutableCompactTransducer.readFromATT(new FileInputStream(
                new File(TEST_DIR + "testMutable8.att")), FstProducer.HFST_ATT);

        assertEquals(Collections.singleton("BDG"), fst1.apply("bdg"));
        assertEquals(Collections.singleton("XXXEFG"), fst1.apply("xxxefg"));
        assertEquals(Collections.singleton("C"), fst1.apply("c"));
        assertEquals(Collections.emptySet(), fst1.apply("bg"));

        assertEquals(Collections.singleton("βδγ"), fst8.apply("BDG"));
        assertEquals(Collections.singleton("χχχεφγ"), fst8.apply("XXXEFG"));
        assertEquals(Collections.singleton("τσ"), fst8.apply("C"));
        assertEquals(Collections.singleton("βγ"), fst8.apply("BG"));

        fst1.compose(fst8);
        fst1.writeToATT(new File(TEST_DIR + "testComposeOut.att"));

        assertEquals(Collections.singleton("βδγ"), fst1.apply("bdg"));
        assertEquals(Collections.singleton("χχχεφγ"), fst1.apply("xxxefg"));
        assertEquals(Collections.singleton("τσ"), fst1.apply("c"));
        assertEquals(Collections.emptySet(), fst1.apply("bg"));
    }

}
