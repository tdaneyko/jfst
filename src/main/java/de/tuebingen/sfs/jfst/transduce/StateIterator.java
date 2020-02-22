package de.tuebingen.sfs.jfst.transduce;

import de.tuebingen.sfs.jfst.symbol.Alphabet;

/**
 * An iterator over the states and transitions of an Transducer.
 */
public interface StateIterator {

    /**
     * @return The total number of states in the underlying Transducer
     */
    int nOfStates();

    /**
     * @return The total number of transitions in the underlying Transducer
     */
    int nOfTransitions();

    /**
     * @return The alphabet of the Transducer
     */
    Alphabet getAlphabet();

    /**
     * @return The id of the start state
     */
    int getStartState();

    /**
     * @return True if there are still states left to iterate through
     */
    boolean hasNextState();

    /**
     * Advance to the next state.
     */
    void nextState();

    /**
     * @return True if the current state is an accepting state
     */
    boolean accepting();

    /**
     * @return True if the current state still has transitions left to iterate through
     */
    boolean hasNextTransition();

    /**
     * Advance to the next transition of this state.
     */
    void nextTransition();

    /**
     * @return The integer id of the input symbol of the current literal transition (-1 if it is an identity transition)
     */
    int inId();

    /**
     * @return The integer id of the output symbol of the current literal transition (-1 if it is an identity transition)
     */
    int outId();

    /**
     * @return The integer id of the to-state of the current transition
     */
    int toId();

}
