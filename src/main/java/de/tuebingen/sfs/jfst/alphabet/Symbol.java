package de.tuebingen.sfs.jfst.alphabet;

/**
 * A symbol in the alphabet of an FST.
 */
public interface Symbol extends Comparable<Object> {

    /**
     * String representation of epsilon (empty symbol)
     */
    String EPSILON_STRING = "\u0000";
    /**
     * Character representation of epsilon (empty symbol)
     */
    char EPSILON_CHAR = '\u0000';
    /**
     * String representation of the identity symbol (copy symbol)
     */
    String IDENTITY_STRING = "\u0001";
    /**
     * Character representation of the identity symbol (copy symbol)
     */
    char IDENTITY_CHAR = '\u0001';

    /**
     * Get the unique id of this symbol in its alphabet.
     * @return The symbol's id
     */
    int getId();

    /**
     * Check whether this is the epsilon symbol.
     * @return True if this is the epsilon symbol
     */
    boolean isEpsilon();

    /**
     * Get the length (number of chars) of this symbol.
     * @return The symbol's length
     */
    int length();

    /**
     * Append this symbol to a StringBuilder.
     * @param s A StringBuilder
     * @return The same StringBuilder after the symbol was appended
     */
    StringBuilder appendTo(StringBuilder s);

    /**
     * Append this symbol to the beginning of a StringBuilder.
     * @param s A StringBuilder
     * @return The same StringBuilder after the symbol was prepended
     */
    StringBuilder prependTo(StringBuilder s);

    /**
     * Check whether this symbol is equivalent to a char (if they were equal if the
     * symbol was converted to a char).
     * @param c A char
     * @return True if the symbol is equivalent to c
     */
    boolean equivalentTo(char c);

    /**
     * Check whether this symbol is equivalent to a string (if they were equal if the
     * symbol was converted to a string).
     * @param s A string
     * @return True if the symbol is equivalent to s
     */
    boolean equivalentTo(String s);

    boolean prefixOf(String s);

    boolean prefixOf(String s, int start);

    boolean startsWith(char c);

    /**
     * Get the string representation of this symbol, as it should be appended to e.g. a StringBuilder.
     * For example, this means that the epsilon should be represented as the empty string here
     * but as EPSILON_STRING in asString()!
     * @return The string representation of this symbol
     */
    @Override
    String toString();

    /**
     * Get the string value of this symbol, i.e. a typecast from the underlying symbol to a string.
     * For example, this means that the epsilon should be represented as EPSILON_STRING here
     * but as the empty string in asString()!
     * @return The string value of this symbol
     */
    String asString();
}
