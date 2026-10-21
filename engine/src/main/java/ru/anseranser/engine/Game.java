package ru.anseranser.engine;

import java.util.List;

/**
 * The public surface of the engine: three methods, and no state of its own.
 *
 * <p>The rules hold nothing between calls: every method takes a state and
 * returns a new one.
 */
public final class Game {

    private Game() {
    }

    /**
     * The first state of a game: the deck is built from the suits in play, the
     * base card of every suit goes on the board of the seat that owns it, the
     * rest is dealt, and the obligatory exchange is applied.
     *
     * <p>The transition reports no events yet.
     */
    public static Transition start(Setup setup) {
        State dealt = Dealing.deal(setup);
        return new Transition(Exchange.apply(dealt), List.of());
    }

    /**
     * The moves the seat to move may choose from; empty when the game is over.
     */
    public static List<Move> legalMoves(State state) {
        return Trick.legalMoves(state);
    }

    /**
     * The result of applying the move at {@code moveIndex} to {@code state}.
     * The index is a position in the list {@link #legalMoves(State)} returns,
     * so an illegal move has no representation.
     *
     * @throws IndexOutOfBoundsException when the index is not a legal move
     */
    public static Transition step(State state, int moveIndex) {
        return new Transition(Trick.step(state, moveIndex), List.of());
    }
}
