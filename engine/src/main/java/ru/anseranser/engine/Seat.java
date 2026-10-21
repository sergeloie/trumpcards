package ru.anseranser.engine;

import java.util.List;

/**
 * One participant at the table: the trump suit it beats with, the cards it
 * holds, the ladder of trump cards on its board and whether it is out of the
 * game.
 *
 * <p>A seat that empties its hand is out of the current round but stays in the
 * game until its board reaches the ace.
 */
public record Seat(Card.Suit trump, List<Card> hand, List<Card> board, boolean out) {

    public Seat {
        hand = List.copyOf(hand);
        board = List.copyOf(board);
    }
}
