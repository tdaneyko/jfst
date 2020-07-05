package de.tuebingen.sfs.jfst.symbol;

import java.util.*;

/**
 * The collection of symbols used by an Transducer.
 *
 * This alphabet stores a single object for each unique char or string that occurs
 * in an Transducer. The Transducer can retrieve references to these wrapper objects for its transitions
 * and reuse them wherever the symbol occurs to save space. (I.e. each unique string or
 * char is stored only once instead of each time when it occurs in a transition.)
 */
public class Alphabet {

    /**
     * String representation of epsilon (empty symbol)
     */
    public static final String EPSILON_STRING = "\u0000";
    /**
     * String representation of the unknown identity symbol (== unknown character mapped to itself)
     */
    public static final String UNKNOWN_IDENTITY_STRING = "\u0001";
    /**
     * String representation of the unknown symbol (== any unknown character)
     */
    public static final String UNKNOWN_STRING = "\u0002";


    // Symbols in alphabetic order
    private final TreeMap<String, Integer> sym2id;
    // Symbols in the order of their ids
    private final List<String> id2sym;
    // Id of the epsilon symbol
    private int epsIdx;
    // Id of the identity symbol
    private int idIdx;
    // Id of the unknown symbol
    private int unknIdx;

    /**
     * Create an empty Alphabet.
     */
    public Alphabet() {
        sym2id = new TreeMap<>();
        id2sym = new ArrayList<>();
        addSymbol(EPSILON_STRING);
        addSymbol(UNKNOWN_IDENTITY_STRING);
        addSymbol(UNKNOWN_STRING);
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
     * @param symbol A string symbol
     * @return True if this Alphabet already contains the symbol
     */
    public boolean contains(String symbol) {
        return sym2id.containsKey(symbol);
    }

    public int epsilonId() {
        return epsIdx;
    }

    public boolean epsilon(int id) {
        return id == epsIdx;
    }

    public int identityId() {
        return idIdx;
    }

    public boolean identity(int id) {
        return id == idIdx;
    }

    public int unknownId() {
        return unknIdx;
    }

    public boolean unknown(int id) {
        return id == unknIdx;
    }

    /**
     * Get the symbol with this id.
     * @param id An id
     * @return The symbol with this id or null if there is no such symbol
     */
    public String getSymbol(int id) {
        if (id < id2sym.size())
            return id2sym.get(id);
        else
            return null;
    }

    /**
     * Find the id of that symbol.
     * @param symbol A string symbol
     * @return The id of the symbol in this alphabet, or -1 if it is not contained
     */
    public int getId(String symbol) {
        return sym2id.getOrDefault(symbol, -1);
    }

    public int getIdOrCreate(String symbol) {
        return sym2id.computeIfAbsent(symbol, x -> addSymbol(symbol));
    }

    public List<String> getPrefixes(String s, int start) {
        List<String> prefixes = new ArrayList<>();
        char c = s.charAt(start);
        for (Map.Entry<String, Integer> entry : sym2id.tailMap(c + "").entrySet()) {
            String sym = entry.getKey();
            if (sym.charAt(0) != c)
                break;
            if (s.startsWith(sym, start))
                prefixes.add(sym);
        }
        return prefixes;
    }

    /**
     * Add a symbol to the alphabet.
     * @param symbol A symbol
     */
    public int addSymbol(String symbol) {
        if (sym2id.containsKey(symbol))
            return sym2id.get(symbol);
        else {
            int id = id2sym.size();
            switch (symbol) {
                case EPSILON_STRING:
                    epsIdx = id;
                    break;
                case UNKNOWN_IDENTITY_STRING:
                    idIdx = id;
                    break;
                case UNKNOWN_STRING:
                    unknIdx = id;
                    break;
            }

            sym2id.put(symbol, id);
            id2sym.add(symbol);
            return id;
        }
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
     * Get the string representations of all symbols in this alphabet as an array,
     * ordered according to their id.
     * @return An array with all symols in this alphabet
     */
    public String[] getSymbols() {
        return id2sym.toArray(new String[0]);
    }

    /**
     * Get the total number of symbols in this alphabet.
     * @return The size of this alphabet
     */
    public int size() {
        return sym2id.size();
    }

}
