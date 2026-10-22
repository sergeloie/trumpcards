package ru.anseranser.desktop;

import ru.anseranser.engine.Move;
import ru.anseranser.engine.State;

import java.util.List;

/**
 * A policy that picks one of the legal moves: the person at the mouse, the
 * rule bot, or anything else that plays the game.
 *
 * <p>The choice is the index of the move in the list the engine gives.</p>
 */
interface MoveChooser {

    /**
     * The index of the chosen move among the legal moves.
     *
     * @throws IllegalArgumentException when the move is not legal
     */
    int choose(State state, List<Move> moves);
}
