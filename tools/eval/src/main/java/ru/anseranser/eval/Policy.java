package ru.anseranser.eval;

import ru.anseranser.engine.Move;
import ru.anseranser.engine.State;

import java.util.List;

/**
 * A policy: the move it chooses, as the index in the list of legal moves.
 *
 * <p>Every policy has this shape — the rule bot and the learned one alike —
 * so the evaluator drives them all the same way and names no type of any of
 * them.
 */
@FunctionalInterface
interface Policy {

    /**
     * The move the policy takes: the index in the list of legal moves.
     *
     * @throws IllegalArgumentException when the game is over and there is no move to choose
     */
    int choose(State state, List<Move> moves);
}
