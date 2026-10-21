package ru.anseranser.engine;

import java.util.ArrayList;
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
     * <p>The transition reports the game that started, the round that began,
     * the cards every seat received and every card the exchange passed.
     */
    public static Transition start(Setup setup) {
        List<Event> events = new ArrayList<>();
        events.add(new Event.GameStarted());

        Transition dealt = Dealing.deal(setup);
        events.addAll(dealt.events());

        Transition exchanged = Exchange.apply(dealt.state());
        events.addAll(exchanged.events());

        return new Transition(exchanged.state(), events);
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
     * <p>The transition reports the card that was played, whether it was a lead
     * or a beat, the pot that was taken, the round that ended, the seat that
     * was eliminated and the game that ended. A step that ends a round reports
     * the round that began after it, with the cards of that round.
     *
     * @throws IndexOutOfBoundsException when the index is not a legal move
     */
    public static Transition step(State state, int moveIndex) {
        return Trick.step(state, moveIndex);
    }
}
