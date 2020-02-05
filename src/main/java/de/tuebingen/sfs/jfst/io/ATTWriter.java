package de.tuebingen.sfs.jfst.io;

import de.tuebingen.sfs.jfst.alphabet.Alphabet;
import de.tuebingen.sfs.jfst.alphabet.Symbol;
import de.tuebingen.sfs.jfst.fst.FST;
import de.tuebingen.sfs.jfst.fst.FSTStateIterator;

import java.io.*;

public class ATTWriter {

    /**
     * Write an FST to an AT&T file.
     * @param file Output file
     * @param fst The FST
     * @throws IOException
     */
    public static void writeFST(File file, FST fst) throws IOException {
        writeFST(file, fst.iter());
    }

    /**
     * Write an FST to an AT&T file.
     * @param file Output file
     * @param states Iterator over states and transitions of an FST
     * @throws IOException
     */
    public static void writeFST(File file, FSTStateIterator states) throws IOException {
        writeFST(file, states, FSTProducer.HFST);
    }

    /**
     * Write an FST to an AT&T file.
     * @param file Output file
     * @param states Iterator over states and transitions of an FST
     * @throws IOException
     */
    public static void writeFST(File file, FSTStateIterator states, FSTProducer producer) throws IOException {
        Alphabet alph = states.getAlphabet();
        int fromState = 0;
        PrintWriter writer = new PrintWriter(file, "UTF-8");
        while (states.hasNextState()) {
            states.nextState();
            while (states.hasNextTransition()) {
                states.nextTransition();
                int toState = states.toId();
                String inSym = (states.identity()) ? producer.identity() : alph.getSymbol(states.inId()).asString();
                String outSym = (states.identity()) ? producer.identity() : alph.getSymbol(states.outId()).asString();
                writer.println(fromState + "\t" + toState + "\t" + inSym + "\t" + outSym);
            }
            if (states.accepting())
                writer.println(fromState);
            fromState++;
        }
        writer.close();
    }

}
