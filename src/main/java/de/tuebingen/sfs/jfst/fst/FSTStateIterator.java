package de.tuebingen.sfs.jfst.fst;

import de.tuebingen.sfs.jfst.alphabet.Alphabet;

/**
 * An iterator over the states and transitions of an FST.
 */
public interface FSTStateIterator {

    /**
     * @return The total number of states in the underlying FST
     */
    int nOfStates();

    /**
     * @return The total number of transitions in the underlying FST
     */
    int nOfTransitions();

    /**
     * @return The alphabet of the FST
     */
    Alphabet getAlphabet();

    /**
     * @return The id of the start state
     */
    int getStartState();

    /**
     * @return The id of the identity symbol in the alphabet
     */
    int getIdentityId();

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
     * @return True if the current transition is an identity transition
     */
    boolean identity();

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
