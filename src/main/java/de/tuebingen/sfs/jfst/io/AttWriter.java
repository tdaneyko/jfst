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
        writeFST(file, states, FstProducer.HFST_ATT);
    }

    /**
     * Write an Transducer to an AT&T file.
     * @param file Output file
     * @param states Iterator over states and transitions of an Transducer
     * @throws IOException
     */
    public static void writeFST(File file, StateIterator states, FstProducer producer) throws IOException {
        Alphabet alph = states.getAlphabet();
        int startState = states.getStartState();
        int fromState = 0;
        PrintWriter writer = new PrintWriter(file, "UTF-8");
        while (states.hasNextState()) {
            states.nextState();
            while (states.hasNextTransition()) {
                states.nextTransition();
                int toState = switchZeroState(states.toId(), startState);
                String inSym = getSymbolRepresentation(states.inId(), alph, producer);
                String outSym = getSymbolRepresentation(states.outId(), alph, producer);
                writer.println(switchZeroState(fromState, startState) + "\t" + toState + "\t" + inSym + "\t" + outSym);
            }
            if (states.accepting())
                writer.println(switchZeroState(fromState, startState));
            fromState++;
        }
        writer.close();
    }

    private static int switchZeroState(int thisState, int otherState) {
        if (thisState == 0)
            return otherState;
        if (thisState == otherState)
            return 0;
        return thisState;
    }

    private static String getSymbolRepresentation(int symId, Alphabet alph, FstProducer producer) {
        if (symId == alph.epsilonId())
            return producer.epsilon();
        else if (symId == alph.unknownId())
            return producer.unknown();
        else if (symId == alph.identityId())
            return producer.identity();
        else
            return alph.getSymbol(symId);
    }

}
