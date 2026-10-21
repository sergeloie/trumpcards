package ru.anseranser.engine;

import java.util.List;

/**
 * The complete description of one game at one point in time: every seat with
 * its hand and its board, the pot, the deck, the seat to move and the position
 * of the random generator.
 *
 * <p>The seat to move has to lead the next trick when it has just beaten the
 * top card of the pot. The turn counter counts the turns of the current round:
 * a turn that beats a card and then leads the next trick counts once.
 *
 * <p>A state alone is enough to continue the game.
 */
public record State(
        List<Seat> seats,
        List<Card> pot,
        List<Card> deck,
        Seat dealer,
        Seat turn,
        boolean leadRequired,
        int turns,
        long rngState) {

    public State {
        seats = List.copyOf(seats);
        pot = List.copyOf(pot);
        deck = List.copyOf(deck);
    }
}
