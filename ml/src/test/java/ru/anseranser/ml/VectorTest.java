package ru.anseranser.ml;

import org.junit.jupiter.api.Test;
import ru.anseranser.engine.Card;
import ru.anseranser.engine.Game;
import ru.anseranser.engine.Move;
import ru.anseranser.engine.RankSet;
import ru.anseranser.engine.Seat;
import ru.anseranser.engine.Setup;
import ru.anseranser.engine.State;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.anseranser.engine.Card.Rank.ACE;
import static ru.anseranser.engine.Card.Rank.EIGHT;
import static ru.anseranser.engine.Card.Rank.JACK;
import static ru.anseranser.engine.Card.Rank.KING;
import static ru.anseranser.engine.Card.Rank.NINE;
import static ru.anseranser.engine.Card.Rank.SEVEN;
import static ru.anseranser.engine.Card.Rank.SIX;
import static ru.anseranser.engine.Card.Rank.TEN;
import static ru.anseranser.engine.Card.Suit.CLUBS;
import static ru.anseranser.engine.Card.Suit.DIAMONDS;
import static ru.anseranser.engine.Card.Suit.HEARTS;
import static ru.anseranser.engine.Card.Suit.SPADES;

/**
 * The vector: a fixed number of values that describe the table to the seat
 * that is about to move, and a mask over the legal moves.
 */
class VectorTest {

    private static final int HAND = 0;
    private static final int POT = 156;
    private static final int OPPONENTS = 312;
    private static final int BOARDS = 330;
    private static final int TABLE = 338;
    private static final int LEGAL_MOVES = 351;

    @Test
    void theVectorHasAFixedLength() {
        Vector four = vector(state());
        Vector two = vector(twoSeatState());
        Vector fiftyTwo = vector(Game.start(
                new Setup(RankSet.TWO_TO_ACE, List.of(SPADES, CLUBS, DIAMONDS, HEARTS), 42L)).state());

        assertEquals(352, Vector.LENGTH, "the length is a constant of the layout");
        assertEquals(352, four.values().length);
        assertEquals(352, two.values().length, "a table of two seats has the same vector");
        assertEquals(352, fiftyTwo.values().length, "the rank set does not change the length");
    }

    @Test
    void theSameSeatAndStateGiveTheSameVector() {
        Vector first = vector(state());
        Vector second = vector(state());

        assertEquals(first, second, "the same state and the same seat give the same vector");
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    void theOpponentsCardsAreNotInTheVector() {
        State changed = stateWith(new Seat(CLUBS, List.of(card(CLUBS, JACK)),
                List.of(card(CLUBS, SIX)), false));

        assertEquals(vector(state()), vector(changed),
                "the cards of an opponent are hidden, so only its count reaches the vector");
    }

    @Test
    void theOwnHandIsInTheVector() {
        State changed = stateWithHand(List.of(card(SPADES, KING), card(CLUBS, NINE)));

        assertEquals(2, Game.legalMoves(changed).size(), "the observer still has two legal moves");
        assertNotEquals(vector(state()), vector(changed),
                "the observer sees its own hand");
    }

    @Test
    void thePotIsInTheVector() {
        State changed = new State(seats(), List.of(card(HEARTS, KING), card(SPADES, TEN)),
                List.of(), clubs(), spades(), true, 0, 42L);

        assertNotEquals(vector(state()), vector(changed), "the pot is public");
    }

    @Test
    void theMaskHoldsTheLegalCards() {
        List<Move> moves = Game.legalMoves(state());

        Vector vector = vector(state());

        assertEquals(2, moves.size(), "the observer may play either card of its hand");
        assertEquals((1L << 5) | (1L << 20), vector.mask(),
                "the mask holds a bit for the seven of spades and for the nine of clubs");
        assertEquals(0L, vector.mask() & (1L << 52), "taking the pot is not legal");
        assertEquals(2f, vector.values()[LEGAL_MOVES], "the vector holds the number of legal moves");
    }

    @Test
    void theMaskHoldsOnlyTheCardsThatCanBePlayed() {
        State state = defenceState();

        Vector vector = vector(state);

        assertEquals(2, Game.legalMoves(state).size(), "the two trumps beat the ten of hearts");
        assertEquals((1L << 5) | (1L << 12), vector.mask(),
                "the mask holds a bit for the seven of spades and for the ace of spades");
        assertEquals(0L, vector.mask() & (1L << 20),
                "the nine of clubs is in the hand, but it cannot beat the ten of hearts");
    }

    @Test
    void theMaskHoldsTakingThePot() {
        State state = takePotState();

        Vector vector = vector(state);

        assertEquals(1, Game.legalMoves(state).size(), "nothing beats the ace of spades");
        assertEquals(1L << 52, vector.mask(), "the only action is to take the pot");
        assertEquals(0L, vector.mask() & (1L << 5), "the seven of spades cannot be played");
    }

    @Test
    void theLayoutIsPinned() {
        Vector vector = vector(state());
        float[] values = vector.values();

        assertValue(1f, values, HAND + 15, "the seven of spades is in the hand");
        assertValue(6f, values, HAND + 16, "the rank of the seven of spades");
        assertValue(1f, values, HAND + 17, "the seven of spades is the observer's trump");
        assertValue(1f, values, HAND + 60, "the nine of clubs is in the hand");
        assertValue(8f, values, HAND + 61, "the rank of the nine of clubs");
        assertValue(0f, values, HAND + 62, "the nine of clubs is not the observer's trump");

        assertValue(1f, values, POT + 141, "the ten of hearts is in the pot");
        assertValue(9f, values, POT + 142, "the rank of the ten of hearts");
        assertValue(0f, values, POT + 143, "the ten of hearts is not the observer's trump");

        assertValue(1f, values, OPPONENTS + 0, "the clubs seat holds one card");
        assertValue(0f, values, OPPONENTS + 1, "the clubs seat does not play spades");
        assertValue(1f, values, OPPONENTS + 2, "the clubs seat plays clubs");
        assertValue(0f, values, OPPONENTS + 5, "the clubs seat is in the game");
        assertValue(1f, values, OPPONENTS + 6, "the diamonds seat holds one card");
        assertValue(1f, values, OPPONENTS + 9, "the diamonds seat plays diamonds");
        assertValue(0f, values, OPPONENTS + 11, "the diamonds seat is in the game");
        assertValue(0f, values, OPPONENTS + 12, "the hearts seat holds no card");
        assertValue(1f, values, OPPONENTS + 16, "the hearts seat plays hearts");
        assertValue(1f, values, OPPONENTS + 17, "the hearts seat is out of the game");

        assertValue(6f, values, BOARDS + 0, "the spades board requires the seven");
        assertValue(0f, values, BOARDS + 1, "the spades board is not complete");
        assertValue(6f, values, BOARDS + 2, "the clubs board requires the seven");
        assertValue(6f, values, BOARDS + 4, "the diamonds board requires the seven");
        assertValue(0f, values, BOARDS + 6, "the hearts board requires nothing");
        assertValue(1f, values, BOARDS + 7, "the hearts board is complete");

        assertValue(1f, values, TABLE + 0, "the observer plays spades");
        assertValue(0f, values, TABLE + 1, "the observer does not play clubs");
        assertValue(1f, values, TABLE + 4, "the seat to move is the observer itself");
        assertValue(0f, values, TABLE + 5, "the seat to move is not the next seat");
        assertValue(0f, values, TABLE + 8, "the dealer is not the observer");
        assertValue(1f, values, TABLE + 9, "the dealer is the next seat");
        assertValue(4f, values, TABLE + 12, "four seats sit at the table");

        assertValue(2f, values, LEGAL_MOVES, "the observer has two legal moves");

        assertEquals((1L << 5) | (1L << 20), vector.mask(),
                "the mask names the legal cards, in the order of the hand block");
    }

    @Test
    void theMaskHoldsEveryLegalActionOfAGame() {
        State state = Game.start(new Setup(RankSet.SIX_TO_ACE,
                List.of(SPADES, CLUBS, DIAMONDS, HEARTS), 7L)).state();
        int steps = 0;

        while (true) {
            List<Move> moves = Game.legalMoves(state);
            if (moves.isEmpty()) {
                break;
            }
            Vector vector = Vector.of(state, state.turn(), moves);

            assertEquals(expectedMask(moves), vector.mask(), "the mask names the legal actions");
            assertEquals(moves.size(), Long.bitCount(vector.mask()),
                    "every legal action has a bit of its own");
            assertEquals(0L, vector.mask() >>> 53, "no bit above taking the pot is ever set");
            assertEquals(Vector.LENGTH, vector.values().length, "the vector keeps its length");

            state = Game.step(state, steps % moves.size()).state();
            steps++;
        }

        assertTrue(steps > 0, "the game was played");
    }

    @Test
    void theDeckIsNotInTheVector() {
        State withDeck = new State(seats(), List.of(card(HEARTS, TEN)),
                List.of(card(DIAMONDS, KING)), clubs(), spades(), true, 0, 42L);

        assertEquals(vector(state()), vector(withDeck),
                "the cards that are still to be dealt are not on the table");
    }

    @Test
    void theSeatThatLooksIsTheOneThatDecides() {
        Vector fromSpades = vector(state());
        Vector fromClubs = Vector.of(state(), clubs(), Game.legalMoves(state()));

        assertNotEquals(fromSpades, fromClubs, "the table looks different from another seat");
    }

    private static void assertValue(float expected, float[] values, int index, String message) {
        assertEquals(expected, values[index], 0f, message + " (at " + index + ")");
    }

    /** The mask the moves describe: a bit for every legal card, and one for taking the pot. */
    private static long expectedMask(List<Move> moves) {
        long mask = 0;
        for (Move move : moves) {
            if (move instanceof Move.Play play) {
                mask |= 1L << cardIndex(play.card());
            } else {
                mask |= 1L << 52;
            }
        }
        return mask;
    }

    /** The bit of a card: the suits in the canonical order, the ranks ascending. */
    private static int cardIndex(Card card) {
        return card.suit().ordinal() * 13 + card.rank().ordinal();
    }

    private static Vector vector(State state) {
        return Vector.of(state, state.turn(), Game.legalMoves(state));
    }

    /** A table of four seats: the observer holds two cards and is about to lead. */
    private static State state() {
        return new State(seats(), List.of(card(HEARTS, TEN)), List.of(), clubs(), spades(),
                true, 0, 42L);
    }

    /** A table where the observer defends the ten of hearts with the trumps that beat it. */
    private static State defenceState() {
        Seat observer = new Seat(SPADES, List.of(card(SPADES, SEVEN), card(CLUBS, NINE),
                card(SPADES, ACE)), List.of(card(SPADES, SIX)), false);
        return new State(List.of(observer, clubs(), diamonds(), hearts()),
                List.of(card(HEARTS, TEN)), List.of(), clubs(), observer, false, 0, 42L);
    }

    /** A table where nothing beats the ace of spades, so the pot is taken. */
    private static State takePotState() {
        return new State(seats(), List.of(card(SPADES, ACE)), List.of(),
                clubs(), spades(), false, 0, 42L);
    }

    private static State stateWith(Seat clubs) {
        return new State(List.of(spades(), clubs, diamonds(), hearts()),
                List.of(card(HEARTS, TEN)), List.of(), clubs, spades(), true, 0, 42L);
    }

    private static State stateWithHand(List<Card> hand) {
        Seat observer = new Seat(SPADES, hand, List.of(card(SPADES, SIX)), false);
        return new State(List.of(observer, clubs(), diamonds(), hearts()),
                List.of(card(HEARTS, TEN)), List.of(), clubs(), observer, true, 0, 42L);
    }

    private static State twoSeatState() {
        Seat spades = seat(SPADES, List.of(card(SPADES, SEVEN)));
        Seat hearts = seat(HEARTS, List.of(card(HEARTS, EIGHT)));
        return new State(List.of(spades, hearts), List.of(), List.of(), hearts, spades, true, 0, 42L);
    }

    private static List<Seat> seats() {
        return List.of(spades(), clubs(), diamonds(), hearts());
    }

    private static Seat spades() {
        return seat(SPADES, List.of(card(SPADES, SEVEN), card(CLUBS, NINE)));
    }

    private static Seat clubs() {
        return seat(CLUBS, List.of(card(CLUBS, EIGHT)));
    }

    private static Seat diamonds() {
        return seat(DIAMONDS, List.of(card(DIAMONDS, KING)));
    }

    private static Seat hearts() {
        return new Seat(HEARTS, List.of(), List.of(card(HEARTS, SIX), card(HEARTS, ACE)), true);
    }

    private static Seat seat(Card.Suit trump, List<Card> hand) {
        return new Seat(trump, hand, List.of(card(trump, SIX)), false);
    }

    private static Card card(Card.Suit suit, Card.Rank rank) {
        return new Card(suit, rank);
    }
}
