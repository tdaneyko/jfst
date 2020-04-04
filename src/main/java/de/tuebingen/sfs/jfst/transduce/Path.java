package de.tuebingen.sfs.jfst.transduce;

public abstract class Path implements Comparable<Path> {

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Path)
            return compareTo((Path) obj) == 0;
        return false;
    }

    @Override
    public int compareTo(Path o) {
        SymbolPairIterator ownIter = iter();
        SymbolPairIterator othIter = o.iter();
        while (ownIter.hasNext() && othIter.hasNext()) {
            ownIter.advance();
            othIter.advance();
            int c = Integer.compare(ownIter.getInSym(), othIter.getInSym());
            if (c != 0)
                return c;
            c = Integer.compare(ownIter.getOutSym(), othIter.getOutSym());
            if (c != 0)
                return c;
            c = Boolean.compare(ownIter.circleStart(), othIter.circleStart());
            if (c != 0)
                return c;
            c = Boolean.compare(ownIter.circleEnd(), othIter.circleEnd());
            if (c != 0)
                return c;
        }
        return Boolean.compare(ownIter.hasNext(), othIter.hasNext());
    }

    public abstract SymbolPairIterator iter();

    public abstract class SymbolPairIterator {

        public abstract boolean hasNext();

        public abstract void advance();

        public abstract int getInSym();

        public abstract int getOutSym();

        public int getSymbolPair() {
            return ((getInSym() << 16) & 0x0000ffff) | getOutSym();
        }

        public abstract boolean circleStart();

        public abstract boolean circleEnd();
    }
}
