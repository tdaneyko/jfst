package de.tuebingen.sfs.jfst.alphabet;

/**
 * A symbol consisting of a single character.
 */
class CharSymbol implements Symbol {

    // The symbol
    private final char symbol;
    // The symbol's id
    private final int id;

    /**
     * Create a new single char symbol.
     * @param symbol The symbol
     * @param id The symbol's id
     */
    CharSymbol(char symbol, int id) {
        this.symbol = symbol;
        this.id = id;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public boolean isEpsilon() {
        return symbol == Symbol.EPSILON_CHAR;
    }

    @Override
    public int length() {
        return (isEpsilon()) ? 0 : 1;
    }

    @Override
    public StringBuilder appendTo(StringBuilder s) {
        if (!isEpsilon())
            s.append(this.symbol);
        return s;
    }

    @Override
    public StringBuilder prependTo(StringBuilder s) {
        if (!isEpsilon())
            s.insert(0, this.symbol);
        return s;
    }

    @Override
    public boolean equivalentTo(char c) {
        return symbol == c;
    }

    @Override
    public boolean equivalentTo(String s) {
        return (isEpsilon() && s.isEmpty()) || (s.length() == 1 && s.charAt(0) == symbol);
    }

    @Override
    public boolean prefixOf(String s) {
        return prefixOf(s, 0);
    }

    @Override
    public boolean prefixOf(String s, int start) {
        return s.length() > start && s.charAt(start) == symbol;
    }

    @Override
    public boolean startsWith(char c) {
        return this.symbol == c;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof CharSymbol && this.symbol == ((CharSymbol) other).symbol;
    }

    @Override
    public int compareTo(Object other) {
        if (other instanceof Character || other instanceof CharSymbol) {
            char o = (other instanceof CharSymbol) ? ((CharSymbol) other).symbol : (Character) other;
            return this.symbol - o;
        }
        else if (other instanceof String || other instanceof MulticharSymbol) {
            String o = (other instanceof MulticharSymbol) ? ((MulticharSymbol) other).asString() : (String) other;
            if (!o.isEmpty()) {
                char oc = o.charAt(0);
                int d = symbol - oc;
                if (d == 0 && o.length() > 1) {
                    return d - 1;
                }
                return d;
            }
        }
        return -1;
    }

    @Override
    public String toString() {
        if (isEpsilon())
            return "";
        return asString();
    }

    @Override
    public String asString() {
        return Character.toString(this.symbol);
    }

    /**
     * Get this symbol as a char.
     * @return The char value of this symbol
     */
    public char asChar() {
        return this.symbol;
    }
}
