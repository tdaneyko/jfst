package de.tuebingen.sfs.jfst.io;

import de.tuebingen.sfs.jfst.symbol.Alphabet;
import de.tuebingen.sfs.jfst.transduce.Transducer;
import de.tuebingen.sfs.jfst.transduce.StateIterator;

import java.io.*;

public class AttWriter {

    /**
     * Write an Transducer to an AT&T file.
     * @param file Output file
     * @param fst The Transducer
     * @throws IOException
     */
    public static void writeFST(File file, Transducer fst) throws IOException {
        writeFST(file, fst.iter());
    }

    /**
     * Write an Transducer to an AT&T file.
     * @param file Output file
     * @param states Iterator over states and transitions of an Transducer
     * @throws IOException
     */
    public static void writeFST(File file, StateIterator states) throws IOException {
        writeFST(file, states, FstProducer.HFST);
    }

    /**
     * Write an Transducer to an AT&T file.
     * @param file Output file
     * @param states Iterator over states and transitions of an Transducer
     * @throws IOException
     */
    public static void writeFST(File file, StateIterator states, FstProducer producer) throws IOException {
        Alphabet alph = states.getAlphabet();
        int fromState = 0;
        PrintWriter writer = new PrintWriter(file, "UTF-8");
        while (states.hasNextState()) {
            states.nextState();
            while (states.hasNextTransition()) {
                states.nextTransition();
                int toState = states.toId();
                String inSym = (states.inId() == alph.identityId()) ? producer.identity() : alph.getSymbol(states.inId());
                String outSym = (states.outId() == alph.identityId()) ? producer.identity() : alph.getSymbol(states.outId());
                writer.println(fromState + "\t" + toState + "\t" + inSym + "\t" + outSym);
            }
            if (states.accepting())
                writer.println(fromState);
            fromState++;
        }
        writer.close();
    }

}
