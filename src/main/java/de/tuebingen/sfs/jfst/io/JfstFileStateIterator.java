package de.tuebingen.sfs.jfst.io;

import de.tuebingen.sfs.jfst.symbol.Alphabet;
import de.tuebingen.sfs.util.bin.BufferedByteReader;
import de.tuebingen.sfs.util.bin.IOUtils;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;

import java.io.IOException;
import java.io.InputStream;

public class JfstFileStateIterator implements FileStateIterator {

    // Control bytes
    private static final byte ACC_BYTE = JfstBinaryWriter.ACCEPTING;
    private static final byte N_ACC_BYTE = JfstBinaryWriter.NONACCEPTING;
    private static final byte END_BYTE = JfstBinaryWriter.STATEEND;

    private BufferedByteReader in;
    private boolean inverse;
    private Alphabet alphabet;

    private int nStates;
    private int startID;
    private int nTrans;

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

    public JfstFileStateIterator(String fileName) {
        this(fileName, false);
    }

    public JfstFileStateIterator(String fileName, boolean inverse) {
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
                // Reached end of state
                boolean swAcc = in.startsWith(ACC_BYTE);
                if (swAcc || in.startsWith(N_ACC_BYTE)) {
                    acc = swAcc;
                    // If this was the last state => end of file
                    if (!hasNextState())
                        eof = true;
                    // Else process next state
                    else
                        stateEnd = true;
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
