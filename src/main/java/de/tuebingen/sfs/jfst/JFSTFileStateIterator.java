package de.tuebingen.sfs.jfst;

import de.tuebingen.sfs.util.bin.BufferedByteReader;
import de.tuebingen.sfs.util.bin.IOUtils;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;

import java.io.IOException;
import java.io.InputStream;

public class JFSTFileStateIterator implements FSTFileStateIterator {

    // Control bytes
    private static final byte ACC_BYTE = BinaryFSTWriter.ACCEPTING;
    private static final byte N_ACC_BYTE = BinaryFSTWriter.NONACCEPTING;
    private static final byte END_BYTE = BinaryFSTWriter.STATEEND;

    private BufferedByteReader in;
    private boolean inverse;
    private Alphabet alphabet;

    private int nStates;
    private int startID;
    private int nTrans;
    private int idIdx;

    private int sBytes;
    private int aBytes;
    private int litSize; // Byte length of a literal transition

    private boolean eof;
    private int s;
    private boolean acc;
    private int t;
    private TIntList inSyms;
    private TIntList outSyms;
    private TIntList toStates;

    public JFSTFileStateIterator(String fileName) {
        this(fileName, false);
    }

    public JFSTFileStateIterator(String fileName, boolean inverse) {
        this.inverse = inverse;

        try {
            InputStream inStream = getClass().getResourceAsStream(fileName);
            int bufferSize = 8192;
            in = new BufferedByteReader(inStream, bufferSize);

            // Create alphabet
            alphabet = new Alphabet();
            while (!in.startsWith(IOUtils.NEWLINE)) {
                String sym = in.nextString();
                alphabet.addSymbol(sym);
            }
            in.skip(2);

            // Add identity symbol
            idIdx = alphabet.size();
            alphabet.addSymbol(Symbol.IDENTITY_STRING);

            // Get number of states and transitions
            nStates = in.popToInt();
            sBytes = IOUtils.bytesNeededFor(nStates-1); // State id size
            startID = in.popToInt(sBytes);
            nTrans = in.popToInt();
            aBytes = IOUtils.bytesNeededFor(alphabet.size()-1); // Symbol id size
            litSize = sBytes + aBytes + aBytes;

            eof = false;
            s = -1;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int nOfStates() {
        return nStates;
    }

    @Override
    public int nOfTransitions() {
        return nTrans;
    }

    @Override
    public Alphabet getAlphabet() {
        return alphabet;
    }

    @Override
    public int getStartState() {
        return startID;
    }

    @Override
    public int getIdentityId() {
        return idIdx;
    }

    @Override
    public boolean hasNextState() {
        return s+1 < nStates;
    }

    @Override
    public void nextState() {
        s++;
        inSyms = new TIntArrayList();
        outSyms = new TIntArrayList();
        toStates = new TIntArrayList();
        t = -1;
        boolean litTrans = true;
        try {
            boolean stateEnd = false;
            while (!stateEnd && !eof) {
                // In literal transition area
                if (litTrans) {
                    // End of literal transitions
                    boolean swAcc = in.startsWith(ACC_BYTE);
                    if (swAcc || in.startsWith(N_ACC_BYTE)) {
                        acc = swAcc;
                        litTrans = false;
                        in.skip(1);
                    }
                    // Add literal transition
                    else if (in.hasNext(litSize)) {
                        int toId = in.popToInt(sBytes);
                        int inSym = in.popToInt(aBytes);
                        int outSym = in.popToInt(aBytes);
                        if (inverse) {
                            inSyms.add(outSym);
                            outSyms.add(inSym);
                        }
                        else {
                            inSyms.add(inSym);
                            outSyms.add(outSym);
                        }
                        toStates.add(toId);
                    }
                    // If there are non enough bytes for a literal transition and the end of literals is not marked,
                    // the end of a valid binary file is reached.
                    else
                        eof = true;
                }
                // In identity transition area
                else {
                    // Reached end of state
                    if (in.startsWith(END_BYTE)) {
                        // Back to literal transition area
                        litTrans = true;
                        // If this was the last state => end of file
                        if (!hasNextState())
                            eof = true;
                        // Else process next state
                        else {
                            stateEnd = true;
                        }
                        in.skip(1);
                    }
                    // Add identity transition
                    else if (in.hasNext(sBytes)) {
                        int toId = in.popToInt(sBytes);
                        inSyms.add(idIdx);
                        outSyms.add(idIdx);
                        toStates.add(toId);
                    }
                    // If there are not enough bytes for an identity transition and the end of identities is not marked,
                    // the end of a valid binary file is reached.
                    else
                        eof = true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
//        System.out.println(s + " " + acc + " " + toStates.size());
    }

    @Override
    public boolean accepting() {
        return acc;
    }

    @Override
    public boolean hasNextTransition() {
        return t+1 < toStates.size();
    }

    @Override
    public void nextTransition() {
        if (hasNextTransition())
            t++;
    }

    @Override
    public boolean identity() {
        return inSyms.get(t) == idIdx && outSyms.get(t) == idIdx;
    }

    @Override
    public int inId() {
        return inSyms.get(t);
    }

    @Override
    public int outId() {
        return outSyms.get(t);
    }

    @Override
    public int toId() {
        return toStates.get(t);
    }

    @Override
    public void close() {
        try {
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        s = nStates;
        t = nTrans;
    }
}
