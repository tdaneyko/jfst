package de.tuebingen.sfs.jfst.transduce;

import java.util.ArrayList;
import java.util.List;

public class Path2 {

    private List<PathSection> path;

    public Path2() {
        path = new ArrayList<>();
    }

    public boolean isEmpty() {
        return path.isEmpty();
    }

    public void prependSection(String inSide, String outSide, boolean cycle) {
        path.add(0, new PathSection(inSide, outSide, cycle));
    }

    public void subtract(Path2 other) {
        int i = 0;
        int j = 0;
        int ki = 0;
        int ko = 0;
        while (i < path.size() && j < other.path.size()) {
            if (other.path.get(j).startsWith(path.get(i), ki, ko)) {

            }
        }
    }

    private class PathSection {

        String inSide;
        String outSide;
        boolean cycle;

        public PathSection(String inSide, String outSide, boolean cycle) {
            this.inSide = inSide;
            this.outSide = outSide;
            this.cycle = cycle;
        }

        public void cutOff(int inTo, int outTo) {
            inSide = inSide.substring(inTo);
            outSide = outSide.substring(outTo);
        }

        public boolean startsWith(PathSection other) {
            return startsWith(other, 0, 0);
        }

        public boolean startsWith(PathSection other, int inStart, int outStart) {
            return this.inSide.startsWith(other.inSide, inStart)
                    && this.outSide.startsWith(other.outSide, outStart);
        }
    }
}
