package de.tuebingen.sfs.jfst;

import de.tuebingen.sfs.jfst.fst.CompactFST;
import de.tuebingen.sfs.jfst.fst.FST;
import de.tuebingen.sfs.jfst.fst.MutableFST;
import de.tuebingen.sfs.jfst.io.FSTProducer;
import de.tuebingen.sfs.util.bin.IOUtils;
import junit.framework.TestCase;

import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FSTTest extends TestCase {

    private static final String TEST_DIR = "src/test/resources/";

    private MutableFST hfst;
    private MutableFST sfst;

    private Map<String, Set<String>> hfstTestSet;
    private Map<String, Set<String>> sfstTestSet;

    @Override
    protected void setUp() throws Exception {
        hfst = MutableFST.readFromATT(new FileInputStream(new File(TEST_DIR + "testHFST.att")), FSTProducer.HFST);
        sfst = MutableFST.readFromATT(new FileInputStream(new File(TEST_DIR + "testSFST.att")), FSTProducer.SFST);

        hfstTestSet = new HashMap<>();

        Set<String> aacxo = new HashSet<>();
        aacxo.add("aayo");
        aacxo.add("bbyo");
        hfstTestSet.put("aacxo", aacxo);

        Set<String> aaxo = new HashSet<>();
        aaxo.add("bbiyo");
        hfstTestSet.put("aaxo", aaxo);

        Set<String> aaxoee = new HashSet<>();
        aaxoee.add("bbiyoee");
        hfstTestSet.put("aaxoee", aaxoee);

        sfstTestSet = new HashMap<>();

        aacxo = new HashSet<>();
        aacxo.add("<+A><+A>y<OMEGA>");
        aacxo.add("bby<OMEGA>");
        sfstTestSet.put("aacxo", aacxo);

        aaxo = new HashSet<>();
        aaxo.add("bbiy<OMEGA>");
        sfstTestSet.put("aaxo", aaxo);

        aaxoee = new HashSet<>();
        aaxoee.add("bbiy<OMEGA><END><END>");
        sfstTestSet.put("aaxoee", aaxoee);
    }

    @Override
    protected void tearDown() throws Exception {
        hfst = null;
        sfst = null;
        hfstTestSet = null;
        sfstTestSet = null;
    }

    public void compare(FST fst1, FST fst2, Map<String, Set<String>> testSet) {
        assertEquals(fst1.nOfStates(), fst2.nOfStates());
        assertEquals(fst1.nOfTransitions(), fst2.nOfTransitions());
        for (String test : testSet.keySet()) {
//            System.err.println(test + " " + fst1.apply(test) + " " + fst2.apply(test));
            assertEquals(fst1.apply(test), fst2.apply(test));
        }
    }

    public void testSimple() throws FileNotFoundException {
        MutableFST test = MutableFST.readFromATT(new FileInputStream(new File(TEST_DIR + "test.att")), FSTProducer.HFST);

        Set<String> expected = new HashSet<>();
        expected.add("bbbbby");
        assertEquals(expected, test.apply("aaaaax"));
    }

    public void testMalScript2AsciiSmall() throws FileNotFoundException {
        CompactFST fst = CompactFST.readFromATT(new FileInputStream(new File(TEST_DIR + "mal-small.att")), FSTProducer.HFST);
        Set<String> expected = new HashSet<>();
        expected.add("vaaka");
        assertEquals(expected, fst.apply("വാക"));
    }

    public void testMalScript2AsciiSmallFromBinary() {
        CompactFST fst = CompactFST.readFromBinary("/mal-small.hfst", FSTProducer.HFST);
        Set<String> expected = new HashSet<>();
        expected.add("vaaka");
        assertEquals(expected, fst.apply("വാക"));
    }

    public void testMalScript2Ascii() throws FileNotFoundException {
        CompactFST fst = CompactFST.readFromATT(new FileInputStream(new File(TEST_DIR + "mal-orth2asciiprnc.att")), FSTProducer.HFST);
        Set<String> expected = new HashSet<>();
        expected.add("vaa;n;nikkuka");
        assertEquals(expected, fst.apply("വാങ്ങിക്കുക")); //TODO
    }

    public void testMalScript2AsciiFromBinary() {
        CompactFST fst = CompactFST.readFromBinary("/mal-orth2asciiprnc.hfst", FSTProducer.HFST);
        Set<String> expected = new HashSet<>();
        expected.add("vaa;n;nikkuka");
        assertEquals(expected, fst.apply("വാങ്ങിക്കുക")); //TODO
    }

    public void testEusScript2IpaFromBinary() {
        CompactFST fst = CompactFST.readFromBinary("/eus.hfst", FSTProducer.HFST);
        Set<String> expected = new HashSet<>();
        expected.add("et͡ʃe");
        assertEquals(expected, fst.apply("etxe"));
    }

    public void testHFST() {
        for (String test : hfstTestSet.keySet()) {
            assertEquals(hfstTestSet.get(test), hfst.apply(test));
        }
    }

    public void testSFST() {
        for (String test : sfstTestSet.keySet())
            assertEquals(sfstTestSet.get(test), sfst.apply(test));
    }

    public void testBinaryFormat() throws FileNotFoundException {
        try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(new File(TEST_DIR + "testSFST.jfst")))) {
            sfst.writeToBinary(out);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try (FileInputStream in = new FileInputStream(new File(TEST_DIR + "testSFST.jfst"))) {
            MutableFST sfst2 = MutableFST.readFromBinary(in);
            compare(sfst, sfst2, sfstTestSet);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(new File(TEST_DIR + "testHFST.jfst")))) {
            hfst.writeToBinary(out);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try (FileInputStream in = new FileInputStream(new File(TEST_DIR + "testHFST.jfst"))) {
            MutableFST hfst2 = MutableFST.readFromBinary(in);
            compare(hfst, hfst2, hfstTestSet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void testCompactFST() throws FileNotFoundException {
        MutableFST sfst = MutableFST.readFromATT(new FileInputStream(new File(TEST_DIR + "testSFST.att")), FSTProducer.SFST);
        try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(new File(TEST_DIR + "testSFST.jfst")))) {
            sfst.writeToBinary(out);
        } catch (IOException e) {
            e.printStackTrace();
        }
        CompactFST sfst2 = CompactFST.readFromBinary("/testSFST.jfst");
        compare(sfst, sfst2, sfstTestSet);

        MutableFST hfst = MutableFST.readFromATT(new FileInputStream(new File(TEST_DIR + "testHFST.att")), FSTProducer.HFST);
        try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(new File(TEST_DIR + "testHFST.jfst")))) {
            hfst.writeToBinary(out);
        } catch (IOException e) {
            e.printStackTrace();
        }
        CompactFST hfst2 = CompactFST.readFromBinary("/testHFST.jfst");
        compare(hfst, hfst2, hfstTestSet);
    }

    public void testMutableToCompact() {
        CompactFST sfst2 = sfst.makeCompact();
        compare(sfst, sfst2, sfstTestSet);

        CompactFST hfst2 = hfst.makeCompact();
        compare(hfst, hfst2, hfstTestSet);
    }

    public void testInverse() {
        try {
            MutableFST sfst2 = MutableFST.readFromATT(new FileInputStream(new File(TEST_DIR + "testSFSTinv.att")), FSTProducer.SFST);
            MutableFST hfst2 = MutableFST.readFromATT(new FileInputStream(new File(TEST_DIR + "testHFSTinv.att")), FSTProducer.HFST);
            MutableFST sfst3 = MutableFST.readFromBinary(new FileInputStream(new File(TEST_DIR + "testSFST.jfst")), true);
            MutableFST hfst3 = MutableFST.readFromBinary(new FileInputStream(new File(TEST_DIR + "testHFST.jfst")), true);
            CompactFST sfst4 = CompactFST.readFromBinary("/testSFST.jfst", true);
            CompactFST hfst4 = CompactFST.readFromBinary("/testHFST.jfst", true);

            for (String test : hfstTestSet.keySet()) {
                for (String res : hfst.apply(test)) {
                    assertTrue(hfst2.apply(res).contains(test));
                    assertTrue(hfst3.apply(res).contains(test));
                    assertTrue(hfst4.apply(res).contains(test));
                }
            }
            for (String test : sfstTestSet.keySet()) {
                for (String res : sfst.apply(test)) {
                    assertTrue(sfst2.apply(res).contains(test));
                    assertTrue(sfst3.apply(res).contains(test));
                    assertTrue(sfst4.apply(res).contains(test));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void testPrefixSearch() {
        try {
            MutableFST mfst = MutableFST.readFromATT(new FileInputStream(new File(TEST_DIR + "testPrefix.att")), FSTProducer.SFST);
            CompactFST cfst = CompactFST.readFromATT(new FileInputStream(new File(TEST_DIR + "testPrefix.att")), FSTProducer.SFST);

            Set<String> abc = new HashSet<>();
            abc.add("abcx");
            abc.add("abcxy");
            abc.add("abcp");
            Set<String> abd = new HashSet<>();
            abd.add("abdef");
            abd.add("abdz");

            assertEquals(abc, mfst.prefixSearch("abc"));
            assertEquals(abd, mfst.prefixSearch("abd"));

            assertEquals(abc, cfst.prefixSearch("abc"));
            assertEquals(abd, cfst.prefixSearch("abd"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void testBytesNeeded() {
        assertEquals(1, IOUtils.bytesNeededFor(100));
        assertEquals(1, IOUtils.bytesNeededFor(127));
        assertEquals(2, IOUtils.bytesNeededFor(128));
        assertEquals(2, IOUtils.bytesNeededFor(1000));
        assertEquals(2, IOUtils.bytesNeededFor(32767));
        assertEquals(3, IOUtils.bytesNeededFor(32768));
        assertEquals(3, IOUtils.bytesNeededFor(8388607));
        assertEquals(4, IOUtils.bytesNeededFor(8388608));
    }

}
