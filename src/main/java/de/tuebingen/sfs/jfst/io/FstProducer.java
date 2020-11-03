package de.tuebingen.sfs.jfst.io;

import de.tuebingen.sfs.jfst.symbol.Alphabet;

/**
 * The original producer of a file. Currently supports SFST, HFST and JFST.
 */
public enum FstProducer {

    /**
     * Marks files produced by the Helsinki Finite-State Toolkit.
     */
    HFST_INTERNAL {
        @Override
        public String epsilon() {
            return "@_EPSILON_SYMBOL_@";
        }

        @Override
        public String unknown() {
            return "@_UNKNOWN_SYMBOL_@";
        }

        @Override
        public String identity() {
            return "@_IDENTITY_SYMBOL_@";
        }

        @Override
        public String space() {
            return "@_SPACE_@";
        }
    },
    HFST_ATT {
        @Override
        public String epsilon() {
            return "@0@";
        }

        @Override
        public String unknown() {
            return "@_UNKNOWN_SYMBOL_@";
        }

        @Override
        public String identity() {
            return "@_IDENTITY_SYMBOL_@";
        }

        @Override
        public String space() {
            return "@_SPACE_@";
        }
    },

    /**
     * Marks files produced by the Stuttgart Finite-State Toolkit.
     */
    SFST {
        @Override
        public String epsilon() {
            return "<>";
        }

        @Override
        public String unknown() {
            return "";
        }

        @Override
        public String identity() {
            return "";
        }

        @Override
        public String space() {
            return " ";
        }
    },

    /**
     * Marks files produced by this Transducer library.
     */
    JFST {
        @Override
        public String epsilon() {
            return Alphabet.EPSILON_STRING;
        }

        @Override
        public String unknown() {
            return Alphabet.UNKNOWN_STRING;
        }

        @Override
        public String identity() {
            return Alphabet.UNKNOWN_IDENTITY_STRING;
        }

        @Override
        public String space() {
            return " ";
        }
    };

    /**
     * Get the epsilon representation of this producer.
     * @return The epsilon representation of this producer
     */
    public abstract String epsilon();

    /**
     * Get the unknown symbol representation of this producer.
     * @return The unknown symbol representation of this producer
     */
    public abstract String unknown();

    /**
     * Get the unknown identity representation of this producer.
     * @return The unknown identity representation of this producer
     */
    public abstract String identity();

    /**
     * Get the whitespace representation of this producer.
     * @return The whitespace representation of this producer
     */
    public abstract String space();

    /**
     * Convert this string found in an AT&amp;T file into the string representation
     * supported by JFST.
     * @param s A string from an AT&amp;T file
     * @return The string as supported by JFST
     */
    public String convert(String s) {
        if (s.equals(epsilon()))
            return Alphabet.EPSILON_STRING;
        else if (s.equals(unknown()))
            return Alphabet.UNKNOWN_STRING;
        else if (s.equals(identity()))
            return Alphabet.UNKNOWN_IDENTITY_STRING;
        else if (s.equals(space()))
            return " ";
        return s;
    }
}
