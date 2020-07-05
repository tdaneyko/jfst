package de.tuebingen.sfs.jfst.io;

import de.tuebingen.sfs.jfst.symbol.Alphabet;
import de.tuebingen.sfs.util.bin.BufferedByteReader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class HfstFileStateIterator implements FileStateIterator {

    private static final boolean VERBOSE = true;

    private static final int UNKNOWN_1 = 0x7EB2FDD6;
    private static final int UNKNOWN_2 = 0x7EB2FB74;

    private Map<String, String> props;
    private CharsetDecoder decoder;

    private int nOfStates;
    private int startState;
    private int nOfTrans;
    private int alphSize;
    private int maxId;

    private Alphabet alphabet;

    private BufferedByteReader in;
    private boolean inverse;

    private int s;
    private boolean acc;
    private int stateTrans;

    private int t;
    private int toState;
    private int inSym;
    private int outSym;

    public HfstFileStateIterator(String fileName) {
        this(fileName, false);
    }

    public HfstFileStateIterator(String fileName, boolean inverse) {
        props = new HashMap<>();
        decoder = StandardCharsets.UTF_8.newDecoder();
        this.inverse = inverse;
        int skip = 0;

        try (InputStream inStream = getClass().getResourceAsStream(fileName)) {
            BufferedByteReader in = new BufferedByteReader(inStream);
            skip = readHeader(in);
            skip += readAlphabet(in);

            int remaining = in.skipRemaining();
            int transBytes = remaining - nOfStates * 12;
            nOfTrans = transBytes / 16;
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        if (VERBOSE) {
            System.err.println(nOfStates + " states (start = " + startState + "), " + nOfTrans + " transitions, "
                    + alphSize + " symbols in alphabet, ids < " + maxId);
        }

        try {
            InputStream inStream = getClass().getResourceAsStream(fileName);
            this.in = new BufferedByteReader(inStream);
            this.in.skip(skip);
            s = -1;
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private int readHeader(BufferedByteReader in) throws IOException {
        ByteBuffer startBytes = ByteBuffer.wrap(in.pop(4));
        String start = decoder.decode(startBytes).toString();
        int skip = 4;
        if (start.equals("HFST")) {
            in.skip(1);
            int headerLength = popHfstInt(in, 2);
            in.skip(1);
            ByteBuffer headerBytes = ByteBuffer.wrap(in.pop(headerLength-1));
            String header = decoder.decode(headerBytes).toString();
            in.skip(1);
            skip += 4 + headerLength;

            int unknown1 = popHfstInt(in);
            if (unknown1 != HfstFileStateIterator.UNKNOWN_1)
                System.err.println("Wrong unknown constant 1: " + unknown1 + " instead of " + HfstFileStateIterator.UNKNOWN_1);
            int vectorLength = popHfstInt(in);
            in.skip(vectorLength);
            int standardLength = popHfstInt(in);
            in.skip(standardLength);
            skip += 12 + vectorLength + standardLength;

            in.skip(16);
            skip += 16;

            startState = popHfstInt(in);
            in.skip(4);
            skip += 8;

            nOfStates = popHfstInt(in);
            in.skip(12);
            skip += 16;

            int unknown2 = popHfstInt(in);
            if (unknown2 != HfstFileStateIterator.UNKNOWN_2)
                System.err.println("Wrong unknown constant 2: " + unknown2 + " instead of " + HfstFileStateIterator.UNKNOWN_2);
            in.skip(4);
            skip += 8;

            maxId = popHfstInt(in);
            in.skip(4);
            skip += 8;

            alphSize = popHfstInt(in);
            in.skip(4);
            skip += 8;
        }
        else {
            System.err.println("Not an HFST file! Begins with '" + start + "'.");
        }
        return skip;
    }

    private int readAlphabet(BufferedByteReader in) throws IOException {
        int skip = 0;
        String[] symbols = new String[maxId];
        for (int i = 0; i < alphSize; i++) {
            int len = popHfstInt(in);
            ByteBuffer symBytes = ByteBuffer.wrap(in.pop(len));
            String symbol = decoder.decode(symBytes).toString();
            int id = popHfstInt(in);
            in.skip(4);
            skip += 12 + len;
//            System.err.println(symbol + " " + id);

            if (symbol.equals(FstProducer.HFST.identity())) {
                symbol = Alphabet.UNKNOWN_IDENTITY_STRING;
            }
            else if (symbol.equals(FstProducer.HFST.unknown())) {
                symbol = Alphabet.UNKNOWN_STRING;
            }
            else if (symbol.equals(FstProducer.HFST.epsilon())) {
                symbol = Alphabet.EPSILON_STRING;
            }

            symbols[id] = symbol;
        }
        alphabet = new Alphabet(symbols);
        return skip;
    }

    private int popHfstInt(BufferedByteReader in) throws IOException {
        return popHfstInt(in, 4);
    }

    private int popHfstInt(BufferedByteReader in, int n) throws IOException {
        int i = 0;
        int j = 0;
        while (j < n) {
            int part = in.popToInt(1);
            part = part << j*8;
            i |= part;
            j++;
        }
        return i;
    }

    @Override
    public Alphabet getAlphabet() {
        return alphabet;
    }

    @Override
    public int nOfStates() {
        return nOfStates;
    }

    @Override
    public int nOfTransitions() {
        return nOfTrans;
    }

    @Override
    public int getStartState() {
        return startState;
    }

    @Override
    public boolean hasNextState() {
        return s+1 < nOfStates;
    }

    @Override
    public void nextState() {
        if (hasNextState()) {
            s++;
            try {
                int accBytes = in.popToInt();
                acc = accBytes == 0;
                stateTrans = popHfstInt(in);
                in.skip(4);
            } catch (IOException e) {
                e.printStackTrace();
            }
            t = -1;
        }
    }

    @Override
    public boolean accepting() {
        return acc;
    }

    @Override
    public boolean hasNextTransition() {
        return t+1 < stateTrans;
    }

    @Override
    public void nextTransition() {
        if (hasNextTransition()) {
            t++;
            try {
                inSym = popHfstInt(in);
                outSym = popHfstInt(in);
                in.skip(4);
                toState = popHfstInt(in);
//                System.err.println(t + " " + alphabet[inSym] + " " + alphabet[outSym] + " " + toState);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public int inId() {
        return (inverse) ? outSym : inSym;
    }

    @Override
    public int outId() {
        return (inverse) ? inSym : outSym;
    }

    @Override
    public int toId() {
        return toState;
    }

    @Override
    public void close() {
        try {
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        s = nOfStates;
        t = nOfTrans;
    }
}
