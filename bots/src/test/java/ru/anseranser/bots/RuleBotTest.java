package ru.anseranser.bots;

import org.junit.jupiter.api.Test;
import ru.anseranser.engine.Card;
import ru.anseranser.engine.Game;
import ru.anseranser.engine.Move;
import ru.anseranser.engine.Seat;
import ru.anseranser.engine.State;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.anseranser.engine.Card.Rank.EIGHT;
import static ru.anseranser.engine.Card.Rank.KING;
import static ru.anseranser.engine.Card.Rank.NINE;
import static ru.anseranser.engine.Card.Rank.SEVEN;
import static ru.anseranser.engine.Card.Rank.TEN;
import static ru.anseranser.engine.Card.Suit.CLUBS;
import static ru.anseranser.engine.Card.Suit.HEARTS;
import static ru.anseranser.engine.Card.Suit.SPADES;

/**
 * The rule bot: a lead takes the first card of the hand that minimises
 * (is-trump, rank) and a defence takes the first of the legal moves, which the
 * engine lists sorted weakest first.
 */
class RuleBotTest {

    @Test
    void theLeadTakesTheWeakestNonTrump() {
        State state = state(SPADES,
                List.of(card(SPADES, SEVEN), card(HEARTS, NINE), card(CLUBS, EIGHT)),
                List.of(), true);

        assertEquals(card(CLUBS, EIGHT), chosen(state),
                "the trump sorts after every other suit, so the eight of clubs is the weakest card");
    }

    @Test
    void theFirstOfTheEqualMinimaWins() {
        State heartsFirst = state(SPADES,
                List.of(card(HEARTS, SEVEN), card(CLUBS, SEVEN), card(HEARTS, EIGHT)),
                List.of(), true);
        State clubsFirst = state(SPADES,
                List.of(card(CLUBS, SEVEN), card(HEARTS, SEVEN)),
                List.of(), true);

        assertEquals(card(HEARTS, SEVEN), chosen(heartsFirst),
                "two cards of the same rank are told apart by their place in the hand");
        assertEquals(card(CLUBS, SEVEN), chosen(clubsFirst),
                "the order of the hand decides between cards of equal strength");
    }

    @Test
    void theLeadTakesTheWeakestTrumpWhenTheHandHoldsOnlyTrumps() {
        State state = state(SPADES, List.of(card(SPADES, KING), card(SPADES, SEVEN)), List.of(), true);

        assertEquals(card(SPADES, SEVEN), chosen(state),
                "among trumps the weaker rank wins, wherever it sits in the hand");
    }

    @Test
    void theDefenceTakesTheFirstOfTheLegalMoves() {
        State state = state(SPADES,
                List.of(card(HEARTS, TEN), card(SPADES, SEVEN), card(SPADES, NINE)),
                List.of(card(HEARTS, SEVEN)), false);

        List<Move> moves = Game.legalMoves(state);

        assertEquals(3, moves.size(), "the ten of hearts and both trumps beat the seven of hearts");
        assertEquals(card(HEARTS, TEN), chosen(state),
                "the weakest defence comes first in the list of legal moves");
    }

    @Test
    void theOnlyMoveIsToTakeThePot() {
        State state = state(SPADES,
                List.of(card(HEARTS, SEVEN), card(CLUBS, EIGHT)),
                List.of(card(SPADES, KING)), false);

        List<Move> moves = Game.legalMoves(state);

        assertEquals(1, moves.size(), "nothing beats the king of spades");
        assertInstanceOf(Move.TakePot.class, moves.get(0));
        assertEquals(0, RuleBot.choose(state, moves));
    }

    @Test
    void theGameOverHasNoMoveToChoose() {
        State state = state(SPADES, List.of(card(SPADES, SEVEN)), List.of(), true);

        assertThrows(IllegalArgumentException.class, () -> RuleBot.choose(state, List.of()));
    }

    /** The state of a table of two seats, with the seat to move holding the hand. */
    private static State state(Card.Suit trump, List<Card> hand, List<Card> pot, boolean leadRequired) {
        Seat mover = new Seat(trump, hand, List.of(), false);
        Seat other = new Seat(trump == SPADES ? CLUBS : SPADES, List.of(), List.of(), false);
        return new State(List.of(mover, other), pot, List.of(), other, mover, leadRequired, 0, 0L);
    }

    /** The card the bot plays in the state. */
    private static Card chosen(State state) {
        List<Move> moves = Game.legalMoves(state);
        return ((Move.Play) moves.get(RuleBot.choose(state, moves))).card();
    }

    private static Card card(Card.Suit suit, Card.Rank rank) {
        return new Card(suit, rank);
    }
}
