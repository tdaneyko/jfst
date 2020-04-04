package de.tuebingen.sfs.jfst.transduce.compact;

import de.tuebingen.sfs.jfst.transduce.Path;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;

public class CompactPath extends Path {

    private static final int getFinal16 = 0x0000ffff;

    private int[] path;
    private boolean[] circleStart;
    private boolean[] circleEnd;
    private int frontPos;
    private int backPos;

    public CompactPath(int length) {
        path = new int[length];
        circleStart = new boolean[length];
        circleEnd = new boolean[length];
        frontPos = 0;
        backPos = length - 1;
    }

    public CompactPath(List<Integer> path, List<Boolean> circleStart, List<Boolean> circleEnd) {
        this.path = path.stream().mapToInt(i -> i).toArray();
        // TODO: Convert boolean lists to arrays?
    }

    public void appendSymPair(int symPair) {
        path[frontPos] = symPair;
        frontPos++;
    }

    public void prependSymPair(int symPair) {
        path[backPos] = symPair;
        backPos--;
    }

    public void markCircle(int circleStart, int circleEnd) {
        this.circleStart[circleStart] = true;
        this.circleEnd[circleEnd] = true;
    }

    public boolean isFilled() {
        return frontPos >= backPos;
    }

    public CompactPath subtract(Path other) {
        List<Integer> newPath = new ArrayList<>();
        List<Boolean> newCircleStart = new ArrayList<>();
        List<Boolean> newCircleEnd = new ArrayList<>();
        Stack<Integer> circleBack = new Stack<>();
        boolean match = true;
        int i = 0;
        SymbolPairIterator iter = other.iter();
        while (iter.hasNext()) {
            iter.advance();
            if (path[i] == iter.getSymbolPair()) {
                if (circleStart[i] && !iter.circleStart()) {
                    match = false;
                    circleBack.push(i);
                }
            }
            else
                return null;
        }

        if (match)
            return null;
        else
            return new CompactPath(newPath, newCircleStart, newCircleEnd);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CompactPath)
            return Arrays.equals(this.path, ((CompactPath) obj).path);
        return super.equals(obj);
    }

    @Override
    public int compareTo(Path o) {
        if (o instanceof CompactPath) {
            CompactPath other = (CompactPath) o;
            for (int i = 0; i < Math.min(this.path.length, other.path.length); i++) {
                int c = Integer.compare(this.path[i], other.path[i]);
                if (c != 0)
                    return c;
            }
            return Integer.compare(this.path.length, other.path.length);
        }
        return super.compareTo(o);
    }

    @Override
    public SymbolPairIterator iter() {
        return new CompactSymbolPairIterator();
    }

    private class CompactSymbolPairIterator extends SymbolPairIterator {

        int i = 0;

        @Override
        public boolean hasNext() {
            return i + 1 < path.length;
        }

        @Override
        public void advance() {
            i++;
        }

        @Override
        public int getInSym() {
            return (path[i] >> 16) & getFinal16;
        }

        @Override
        public int getOutSym() {
            return path[i] & getFinal16;
        }

        @Override
        public int getSymbolPair() {
            return path[i];
        }

        @Override
        public boolean circleStart() {
            return circleStart[i];
        }

        @Override
        public boolean circleEnd() {
            return circleEnd[i];
        }
    }
}
