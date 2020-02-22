package de.tuebingen.sfs.jfst.io;

import de.tuebingen.sfs.jfst.symbol.Alphabet;
import de.tuebingen.sfs.jfst.transduce.Transducer;
import de.tuebingen.sfs.jfst.transduce.StateIterator;
import de.tuebingen.sfs.util.bin.IOUtils;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;

import java.io.IOException;
import java.io.OutputStream;

/**
 * A class for writing a JFST object to a binary file.
 */
public class JfstBinaryWriter {

    /**
     * Control byte that marks the end of literal transitions of an accepting state
     */
    public static final byte ACCEPTING = (byte) 0b11111111;
    /**
     * Control byte that marks the end of literal transitions of a non-accepting state
     */
    public static final byte NONACCEPTING = (byte) 0b11111110;
    /**
     * Control byte that marks the end of a state
     */
    public static final byte STATEEND = (byte) 0b11111111;

    /**
     * Write an Transducer to a binary file.
     * @param out Output file
     * @param fst The Transducer
     * @throws IOException
     */
    public static void writeFST(OutputStream out, Transducer fst) throws IOException {
        writeFST(out, fst.iter(), fst.getAlphabet());
    }

    /**
     * Write an Transducer to a binary file.
     * @param out Output file
     * @param states Ierator over states and transitions of an Transducer
     * @param alphabet Symbols used by that Transducer
     * @throws IOException
     */
    public static void writeFST(OutputStream out, StateIterator states, Alphabet alphabet) throws IOException {
        int startID = states.getStartState();
        int nStates = states.nOfStates();
        int nTrans = states.nOfTransitions();
        int nSyms = alphabet.size();
        int a = IOUtils.bytesNeededFor(nSyms-1); // Symbol id size
        int s = IOUtils.bytesNeededFor(nStates-1); // State id size

        // Write alphabet to file
        for (String sym : alphabet.getSymbols()) {
            IOUtils.writeAsBytes(sym, out);
        }
        // Write extra newline to mark end of alphabet
        IOUtils.writeNewline(out);

        // Write number of states
        IOUtils.writeInt(nStates, out);

        // Write start id
        IOUtils.writeIntTruncated(startID, s, out);

        // Write number of transitions
        IOUtils.writeInt(nTrans, out);

        // Write transitions
        while (states.hasNextState()) {
            states.nextState();
            // Store identity transitions to write them later
            TIntList identityTransitions = new TIntArrayList();

            while (states.hasNextTransition()) {
                states.nextTransition();
                // Save identity transitions for later
//                if (states.inId() == alphabet.identityId())
//                    identityTransitions.add(states.toId());
                // Write literal transition
//                else {
                    int toId = states.toId();
                    int inSym = states.inId();
                    int outSym = states.outId();
                    int k = s + a + a - 1;
                    // Convert transition into byte array
                    byte[] transBytes = new byte[k + 1];
                    while (k >= s + a) {
                        transBytes[k] = (byte) outSym;
                        outSym = outSym >> 8;
                        k--;
                    }
                    while (k >= s) {
                        transBytes[k] = (byte) inSym;
                        inSym = inSym >> 8;
                        k--;
                    }
                    while (k >= 0) {
                        transBytes[k] = (byte) toId;
                        toId = toId >> 8;
                        k--;
                    }
                    out.write(transBytes);
//                }
            }
            // Write accepting/non-accepting
            out.write((states.accepting()) ? ACCEPTING : NONACCEPTING);
        }

        out.close();
    }

}
