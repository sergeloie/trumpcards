package ru.anseranser.ml;

import ru.anseranser.engine.Card;
import ru.anseranser.engine.Move;
import ru.anseranser.engine.Seat;
import ru.anseranser.engine.State;

import java.util.Arrays;
import java.util.List;

/**
 * The numbers a learned policy reads: a fixed number of values that describe
 * the table to the seat that is about to move, and a mask over the legal moves.
 *
 * <p>The vector holds {@value #LENGTH} values, in this order:
 *
 * <ol>
 *   <li>the hand of the observer: one slot per card of the full deck, the
 *       suits as {@link Card.Suit} declares them and the ranks ascending,
 *       three values per slot: whether the card is in the hand, its rank and
 *       whether it is the observer's trump;</li>
 *   <li>the pot: the same slots and the same three values;</li>
 *   <li>the opponents: one slot per seat that could sit at the table besides
 *       the observer, in seating order after it, six values per slot: the
 *       number of cards the opponent holds, its suit as four values of which
 *       one is set, and whether it is out of the game;</li>
 *   <li>the boards: one slot per suit, two values: the rank the board
 *       requires next, and whether its ladder is complete;</li>
 *   <li>the table: thirteen values: the observer's trump suit as four values
 *       of which one is set, the seat to move as the distance in seats from
 *       the observer, the dealer as the distance in seats from the observer,
 *       and the number of seats at the table;</li>
 *   <li>the number of legal moves.</li>
 * </ol>
 *
 * <p>A rank is held as its ordinal plus one, so a zero means that there is no
 * rank: the card is not in the hand, or the board requires nothing.
 *
 * <p>The mask holds a bit for every move of the list, so a policy can take the
 * maximum over the legal moves alone. There is one legal move for every card
 * of the observer's hand, or the single move to take the pot, so the number of
 * legal moves is at most the size of a hand, and a hand holds at most the
 * cards of the full deck, so {@value #MASK_BITS} bits are enough.
 */
public record Vector(float[] values, long mask) {

    /** The number of bits in the mask. */
    public static final int MASK_BITS = 64;

    private static final int RANKS = Card.Rank.values().length;
    private static final int CARDS = Card.Suit.values().length * RANKS;
    private static final int SUIT_VALUES = Card.Suit.values().length;

    private static final int CARD_VALUES = 3;
    private static final int CARD_IN_HAND = 0;
    private static final int CARD_RANK = 1;
    private static final int CARD_TRUMP = 2;

    private static final int OPPONENT_VALUES = 6;
    private static final int OPPONENT_COUNT = 0;
    private static final int OPPONENT_SUIT = 1;
    private static final int OPPONENT_OUT = OPPONENT_SUIT + SUIT_VALUES;

    private static final int BOARD_VALUES = 2;
    private static final int BOARD_RANK = 0;
    private static final int BOARD_COMPLETE = 1;

    private static final int TRUMP_VALUES = SUIT_VALUES;
    private static final int MOVER_VALUES = 4;
    private static final int DEALER_VALUES = 4;
    private static final int TABLE_VALUES = TRUMP_VALUES + MOVER_VALUES + DEALER_VALUES + 1;

    private static final int HAND = 0;
    private static final int POT = HAND + CARDS * CARD_VALUES;
    private static final int OPPONENTS = POT + CARDS * CARD_VALUES;
    private static final int BOARDS = OPPONENTS + 3 * OPPONENT_VALUES;
    private static final int TABLE = BOARDS + SUIT_VALUES * BOARD_VALUES;
    private static final int TRUMP = TABLE;
    private static final int MOVER = TRUMP + TRUMP_VALUES;
    private static final int DEALER = MOVER + MOVER_VALUES;
    private static final int SEAT_COUNT = DEALER + DEALER_VALUES;
    private static final int LEGAL_MOVES = SEAT_COUNT + 1;

    /** The number of values in the vector. */
    public static final int LENGTH = LEGAL_MOVES + 1;

    public Vector {
        values = values.clone();
    }

    /**
     * The vector of the state as the seat sees it, and the mask over the legal
     * moves.
     *
     * @throws IllegalArgumentException when the seat is not at the table, or
     *         when there are more legal moves than the mask holds
     */
    public static Vector of(State state, Seat seat, List<Move> moves) {
        if (moves.size() > MASK_BITS) {
            throw new IllegalArgumentException("a mask of " + MASK_BITS + " bits holds "
                    + moves.size() + " moves at most");
        }
        Observation observation = Observation.of(state, seat, moves);
        float[] values = new float[LENGTH];
        writeCards(values, HAND, observation.hand(), observation.trump());
        writeCards(values, POT, observation.pot(), observation.trump());
        writeOpponents(values, observation);
        writeBoards(values, observation);
        writeTable(values, observation);
        values[LEGAL_MOVES] = observation.legalMoveCount();
        return new Vector(values, mask(moves.size()));
    }

    @Override
    public float[] values() {
        return values.clone();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Vector vector
                && mask == vector.mask
                && Arrays.equals(values, vector.values);
    }

    @Override
    public int hashCode() {
        return 31 * Arrays.hashCode(values) + Long.hashCode(mask);
    }

    /** The slot of the card: the suits in the canonical order, the ranks ascending. */
    private static int cardIndex(Card card) {
        return card.suit().ordinal() * RANKS + card.rank().ordinal();
    }

    /** The cards of a pile: in the hand, or in the pot. */
    private static void writeCards(float[] values, int offset, List<Card> cards, Card.Suit trump) {
        for (Card card : cards) {
            int base = offset + cardIndex(card) * CARD_VALUES;
            values[base + CARD_IN_HAND] = 1f;
            values[base + CARD_RANK] = card.rank().ordinal() + 1;
            values[base + CARD_TRUMP] = card.suit() == trump ? 1f : 0f;
        }
    }

    /** The opponents: their count, their suit and whether they are out of the game. */
    private static void writeOpponents(float[] values, Observation observation) {
        List<Observation.Opponent> opponents = observation.opponents();
        for (int i = 0; i < opponents.size(); i++) {
            Observation.Opponent opponent = opponents.get(i);
            int base = OPPONENTS + i * OPPONENT_VALUES;
            values[base + OPPONENT_COUNT] = opponent.cardCount();
            values[base + OPPONENT_SUIT + opponent.suit().ordinal()] = 1f;
            values[base + OPPONENT_OUT] = opponent.out() ? 1f : 0f;
        }
    }

    /** The boards: the rank they require next, and whether they are complete. */
    private static void writeBoards(float[] values, Observation observation) {
        List<Observation.Board> boards = observation.boards();
        for (int i = 0; i < boards.size(); i++) {
            Observation.Board board = boards.get(i);
            int base = BOARDS + i * BOARD_VALUES;
            Card.Rank required = board.requiredRank();
            values[base + BOARD_RANK] = required == null ? 0f : required.ordinal() + 1;
            values[base + BOARD_COMPLETE] = board.complete() ? 1f : 0f;
        }
    }

    /** The table: the trump suit, the seat to move, the dealer and the seat count. */
    private static void writeTable(float[] values, Observation observation) {
        values[TRUMP + observation.trump().ordinal()] = 1f;
        values[MOVER + observation.seatToMoveOffset()] = 1f;
        values[DEALER + observation.dealerOffset()] = 1f;
        values[SEAT_COUNT] = observation.seatCount();
    }

    /** The mask: a bit for every legal move. */
    private static long mask(int moves) {
        return moves >= MASK_BITS ? -1L : (1L << moves) - 1;
    }
}
