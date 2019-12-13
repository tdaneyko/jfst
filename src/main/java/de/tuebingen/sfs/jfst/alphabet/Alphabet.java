package de.tuebingen.sfs.jfst.alphabet;

import java.util.*;

/**
 * The collection of symbols used by an FST.
 *
 * This alphabet stores a single object for each unique char or string that occurs
 * in an FST. The FST can retrieve references to these wrapper objects for its transitions
 * and reuse them wherever the symbol occurs to save space. (I.e. each unique string or
 * char is stored only once instead of each time when it occurs in a transition.)
 */
public class Alphabet {

    // Symbols in alphabetic order
    private final List<Symbol> alphabet;
    // Symbols in the order of their ids
    private final List<Symbol> id2sym;

    /**
     * Create an empty Alphabet.
     */
    public Alphabet() {
        alphabet = new ArrayList<>();
        id2sym = new ArrayList<>();
    }

    /**
     * Create an Alphabet with symbols.
     */
    public Alphabet(String[] symbols) {
        this();
        addSymbols(symbols);
    }

    /**
     * Create an Alphabet with symbols.
     */
    public Alphabet(Iterable<String> symbols) {
        this();
        addSymbols(symbols);
    }

    /**
     * Checks whether this Alphabet contains a symbol.
     * @param symbol A char symbol
     * @return True if this Alphabet already contains the symbol
     */
    public boolean contains(char symbol) {
        return Collections.binarySearch(alphabet, symbol) >= 0;
    }

    /**
     * Checks whether this Alphabet contains a symbol.
     * @param symbol A string symbol
     * @return True if this Alphabet already contains the symbol
     */
    public boolean contains(String symbol) {
        return Collections.binarySearch(alphabet, symbol) >= 0;
    }

    /**
     * Get the Symbol object associated with this string. If the Alphabet does not
     * contain the symbol yet, it will be added.
     * @param symbol A string symbol
     * @return The Symbol object associated with this string
     */
    public Symbol getSymbol(String symbol) {
        int i = Collections.binarySearch(alphabet, symbol);
        if (i < 0) {
            i = -(i+1);
            return addSymbol(symbol, i);
        }
        else
            return alphabet.get(i);
    }

    /**
     * Get the symbol with this id.
     * @param id An id
     * @return The symbol with this id or null if there is no such symbol
     */
    public Symbol getSymbol(int id) {
        if (id < id2sym.size())
            return id2sym.get(id);
        else
            return null;
    }

    private void setSymbol(int id, String symbol) {
        if (id >= id2sym.size())
            addSymbol(symbol);
        else {
            Symbol sym = id2sym.get(id);
            int i = Collections.binarySearch(alphabet, sym);
            Symbol newSym = createSymbol(symbol, id);
            id2sym.set(id, newSym);
            alphabet.set(i, newSym);
        }
    }

    public List<Symbol> getPrefixes(String s, int start) {
        List<Symbol> prefixes = new ArrayList<>();
        char c = s.charAt(start);
//        int i = Collections.binarySearch(alphabet, c);
//        if (i < 0)
//            i = -i - 1;
        int i = 0;
        while (i < alphabet.size() && !alphabet.get(i).startsWith(c)) {
            i++;
        }
            for (int j = i; j < alphabet.size() && alphabet.get(j).startsWith(c); j++) {
                Symbol sym = alphabet.get(j);
                if (sym.prefixOf(s, start))
                    prefixes.add(sym);
            }
        return prefixes;
    }

    /**
     * Add a symbol to the alphabet. Calls getSymbol() internally.
     * @param symbol A symbol
     */
    public void addSymbol(String symbol) {
        getSymbol(symbol);
    }

    /**
     * Add multiple symbols to the alphabet. Calls getSymbol() internally.
     * @param symbols An array of symbols
     */
    public void addSymbols(String[] symbols) {
        int i = 0;
        for (String sym : symbols) {
            if (sym == null)
                addSymbol("NULL"+(i++));
            else
                addSymbol(sym);
        }
    }

    /**
     * Add multiple symbols to the alphabet. Calls getSymbol() internally.
     * @param symbols A list of symbols
     */
    public void addSymbols(Iterable<String> symbols) {
        for (String sym : symbols) {
            if (sym == null)
                addSymbol("NULL");
            else
                addSymbol(sym);
        }
    }

    /**
     * Insert a symbol at index i.
     * @param symbol A symbol
     * @param i An index in alphabet
     * @return The created Symbol object
     */
    private Symbol addSymbol(String symbol, int i) {
        int id = id2sym.size();
        Symbol s = createSymbol(symbol, id);
        alphabet.add(i, s);
        id2sym.add(s);
        return s;
    }

    private Symbol createSymbol(String symbol, int id) {
        return (symbol.length() == 1) ? new CharSymbol(symbol.charAt(0), id) : new MulticharSymbol(symbol, id);
    }

    /**
     * Get the string representations of all symbols in this alphabet as an array, ordered according to their id.
     * @return An array with all symols in this alphabet
     */
    public String[] getSymbols() {
        return id2sym.stream().map(Symbol::asString).toArray(String[]::new);
    }

    /**
     * Find the id of that symbol.
     * @param symbol A string symbol
     * @return The id of the symbol in this alphabet, or -1 if it is not contained
     */
    public int idOf(String symbol) {
        return (contains(symbol)) ? getSymbol(symbol).getId() : -1;
    }

    /**
     * Get the total number of symbols in this alphabet.
     * @return The size of this alphabet
     */
    public int size() {
        return alphabet.size();
    }

}
