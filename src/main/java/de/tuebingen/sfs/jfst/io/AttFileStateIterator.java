package de.tuebingen.sfs.jfst.io;

import de.tuebingen.sfs.jfst.symbol.Alphabet;
import de.tuebingen.sfs.jfst.transduce.compact.CompactTransition;
import de.tuebingen.sfs.util.string.StringUtils;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class AttFileStateIterator implements FileStateIterator {

    private Alphabet alphabet;
    private int nState;
    private int nTrans;
    private int start;

    private TIntObjectMap<List<CompactTransition>> transitions;
    private List<CompactTransition> currentTransitions;
    private TIntSet accepting;
    private int s = -1;
    private int t = -1;

    public AttFileStateIterator(InputStream in, FstProducer producer, boolean inverse) {
        alphabet = new Alphabet();
        nTrans = 0;
        start = 0;
        try (BufferedReader read = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            // State-to-transitions mapping
            transitions = new TIntObjectHashMap<>();
            accepting = new TIntHashSet();
            for (String line = read.readLine(); line != null; line = read.readLine()) {
                if (!line.isEmpty()) {
                    String[] fields = StringUtils.split(line, '\t');
                    if (fields.length > 0) {
                        // Get or create state with id
                        int id = Integer.parseInt(fields[0]);
                        if (!transitions.containsKey(id))
                            transitions.put(id, new ArrayList<>());
                        // Set accepting...
                        if (fields.length == 1 || fields.length == 2)
                            accepting.add(id);
                        // ...or add transition
                        else if (fields.length == 4 || fields.length == 5) {
                            int toId = Integer.parseInt(fields[1]);
                            String inSym = (inverse) ? fields[3] : fields[2];
                            String outSym = (inverse) ? fields[2] : fields[3];
                            transitions.get(id).add(new CompactTransition(
                                    alphabet.getIdOrCreate(producer.convert(inSym)),
                                    alphabet.getIdOrCreate(producer.convert(outSym)),
                                    toId));
                            nTrans++;
                        }
                    }
                }
            }
            nState = transitions.size();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() {

    }

    @Override
    public int nOfStates() {
        return nState;
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
        return start;
    }

    @Override
    public boolean hasNextState() {
        return s + 1 < transitions.size();
    }

    @Override
    public void nextState() {
        s++;
        currentTransitions = transitions.get(s);
        t = -1;
    }

    @Override
    public boolean accepting() {
        return accepting.contains(s);
    }

    @Override
    public boolean hasNextTransition() {
        return t + 1 < currentTransitions.size();
    }

    @Override
    public void nextTransition() {
        t++;
    }

    @Override
    public int inId() {
        return currentTransitions.get(t).getInSym();
    }

    @Override
    public int outId() {
        return currentTransitions.get(t).getOutSym();
    }

    @Override
    public int toId() {
        return currentTransitions.get(t).getToState();
    }
}
