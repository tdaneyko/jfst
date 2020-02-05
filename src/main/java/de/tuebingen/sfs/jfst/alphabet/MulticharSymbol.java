package de.tuebingen.sfs.jfst.alphabet;

/**
 * A symbol consisting of multiple characters.
 */
class MulticharSymbol implements Symbol {

    // The symbol
    private final String symbol;
    // The symbol's id
    private final int id;

    /**
     * Create a new multichar symbol.
     * @param symbol The symbol
     * @param id The symbol's id
     */
    MulticharSymbol(String symbol, int id) {
        this.symbol = symbol;
        this.id = id;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public boolean isEpsilon() {
        return false;
    }

    @Override
    public int length() {
        return symbol.length();
    }

    @Override
    public StringBuilder appendTo(StringBuilder s) {
        s.append(this.symbol);
        return s;
    }

    @Override
    public StringBuilder prependTo(StringBuilder s) {
        s.insert(0, this.symbol);
        return s;
    }

    @Override
    public boolean equivalentTo(char c) {
        return symbol.length() == 1 && symbol.charAt(0) == c;
    }

    @Override
    public boolean equivalentTo(String s) {
        return symbol.equals(s);
    }

    @Override
    public boolean prefixOf(String s) {
        return prefixOf(s, 0);
    }

    @Override
    public boolean prefixOf(String s, int start) {
        return s.startsWith(symbol, start);
    }

    @Override
    public boolean startsWith(char c) {
        return symbol.length() > 0 && symbol.charAt(0) == c;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof MulticharSymbol && this.symbol.equals(((MulticharSymbol) other).symbol);
    }

    @Override
    public int compareTo(Object other) {
        if (other instanceof MulticharSymbol)
            return this.symbol.compareTo(((MulticharSymbol) other).symbol);
        else if (other instanceof String)
            return this.symbol.compareTo((String) other);
        else if (other instanceof Character || other instanceof CharSymbol) {
            char o = (other instanceof CharSymbol) ? ((CharSymbol) other).asChar() : (Character) other;
            char c = this.symbol.charAt(0);
            int d = c - o;
            if (this.symbol.length() == 1 || d > 0)
                return d;
        }
        return -1;
    }

    @Override
    public String toString() {
        return this.symbol;
    }

    @Override
    public String asString() {
        return this.symbol;
    }
}
