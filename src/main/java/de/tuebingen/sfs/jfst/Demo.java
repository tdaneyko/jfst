package de.tuebingen.sfs.jfst;

import java.io.*;

class Demo {
    public static void main(String[] args) {
        Runtime r = Runtime.getRuntime();
        long memBefore = r.totalMemory() - r.freeMemory();
        long timBefore = System.currentTimeMillis();
//        MutableFST fst = null;
//        try {
//            fst = new MutableFST(new FileInputStream(new File("src/test/resources/zmorge.att")), FSTProducer.SFST, true);
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }
        long timAfter = System.currentTimeMillis();
//        r.gc();
        long memAfter = r.totalMemory() - r.freeMemory();
//        System.err.println("AT&T Time: " + (timAfter - timBefore));
//        System.err.println("AT&T Memory: " + (memAfter - memBefore));
//        for (String s : fst.apply("mich"))
//            System.err.println(s);
//        for (String s : fst.apply("Vermittlungsgespräche"))
//            System.err.println(s);
//        for (String s : fst.apply("überragenderen"))
//            System.err.println(s);
//
//        try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(new File("src/test/resources/zmorge.jfst")))) {
//            fst.writeToBinary(out);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        r.gc();

        memBefore = r.totalMemory() - r.freeMemory();
        timBefore = System.currentTimeMillis();
        CompactFST fst2 = CompactFST.readFromBinary("/zmorge.jfst");
        timAfter = System.currentTimeMillis();
        r.gc();
        memAfter = r.totalMemory() - r.freeMemory();
        System.err.println("JFST Time: " + (timAfter - timBefore));
        System.err.println("JFST Memory: " + (memAfter - memBefore));
        for (String s : fst2.apply("mich"))
            System.err.println(s);
        for (String s : fst2.apply("Vermittlungsgespräche"))
            System.err.println(s);
        for (String s : fst2.apply("ging"))
            System.err.println(s);
        timBefore = System.currentTimeMillis();
        for (String s : fst2.apply("überragenderen"))
            System.err.println(s);
        timAfter = System.currentTimeMillis();
        System.err.println("Lookup Time: " + (timAfter - timBefore));
    }
}
