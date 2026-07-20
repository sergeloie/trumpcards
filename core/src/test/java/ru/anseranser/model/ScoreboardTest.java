package ru.anseranser.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import ru.anseranser.model.DeckSize;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Scoreboard} — extracted from {@code Game} in Stage 4.
 * Covers the obligatory-card ladder ({@code nextRequiredRank}) and the
 * ACE-elimination rule, in isolation from dealing/round flow.
 */
class ScoreboardTest {

    @Test
    void init_placesSixOfEachSuit() {
        Dealer dealer = new Dealer();
        Scoreboard sb = new Scoreboard();
        sb.init(dealer.deck(), Card.Rank.SIX);

        for (Card.Suit suit : Card.Suit.values()) {
            assertEquals(1, sb.snapshot().get(suit).size(),
                    "Each stack should hold exactly one card after init");
            Card top = sb.snapshot().get(suit).get(0);
            assertEquals(Card.Rank.SIX, top.rank());
            assertEquals(suit, top.suit());
        }

        // Deck is reduced by 4 (the four SIXes moved to the scoreboard).
        assertEquals(32, dealer.deck().size());
    }

    @Test
    void init_placesTwoOfEachSuit_forFiftyTwoDeck() {
        Dealer dealer = new Dealer(DeckSize.FIFTY_TWO);
        Scoreboard sb = new Scoreboard();
        sb.init(dealer.deck(), Card.Rank.TWO);

        for (Card.Suit suit : Card.Suit.values()) {
            assertEquals(1, sb.snapshot().get(suit).size(),
                    "Each stack should hold exactly one card after init");
            Card top = sb.snapshot().get(suit).get(0);
            assertEquals(Card.Rank.TWO, top.rank());
            assertEquals(suit, top.suit());
        }

        // Deck is reduced by 4 (the four TWOs moved to the scoreboard).
        assertEquals(48, dealer.deck().size());
    }

    @Test
    void nextRequiredRank_ladderAdvancesAndTerminates() {
        Dealer dealer = new Dealer();
        Scoreboard sb = new Scoreboard();
        sb.init(dealer.deck(), Card.Rank.SIX);

        // After SIX: expect SEVEN, then ... up to KING. ACE is the terminal
        // (the ladder owner keeps the ACE in hand, it is never exchanged).
        Card.Rank expected = Card.Rank.SEVEN;
        Card.Rank[] ladder = {
                Card.Rank.SEVEN, Card.Rank.EIGHT, Card.Rank.NINE, Card.Rank.TEN,
                Card.Rank.JACK, Card.Rank.QUEEN, Card.Rank.KING
        };
        for (Card.Rank r : ladder) {
            assertEquals(r, sb.nextRequiredRank(Card.Suit.SPADES),
                    "nextRequiredRank should advance the ladder");
            // Sim: push the "next required" card to advance the ladder.
            sb.pushAndEliminates(new Card(Card.Suit.SPADES, r));
            expected = r;
        }
        assertNotNull(expected);
        // After KING is pushed, the next required rank is ACE — the ladder owner
        // keeps the ACE in hand (it is never exchanged), so ACE is the terminal
        // sentinel returned by nextRequiredRank.
        assertEquals(Card.Rank.ACE, sb.nextRequiredRank(Card.Suit.SPADES),
                "After KING the ladder reaches ACE (terminal sentinel)");
    }

    @Test
    void nextRequiredRank_fiftyTwoDeck_ladderRunsTwoToAce() {
        Dealer dealer = new Dealer(DeckSize.FIFTY_TWO);
        Scoreboard sb = new Scoreboard();
        sb.init(dealer.deck(), Card.Rank.TWO);

        // After TWO the ladder advances THREE, FOUR, FIVE, ... up to KING, then ACE.
        Card.Rank[] ladder = {
                Card.Rank.THREE, Card.Rank.FOUR, Card.Rank.FIVE,
                Card.Rank.SIX, Card.Rank.SEVEN, Card.Rank.EIGHT, Card.Rank.NINE,
                Card.Rank.TEN, Card.Rank.JACK, Card.Rank.QUEEN, Card.Rank.KING
        };
        for (Card.Rank r : ladder) {
            assertEquals(r, sb.nextRequiredRank(Card.Suit.SPADES),
                    "nextRequiredRank should advance the ladder");
            sb.pushAndEliminates(new Card(Card.Suit.SPADES, r));
        }
        assertEquals(Card.Rank.ACE, sb.nextRequiredRank(Card.Suit.SPADES),
                "After KING the ladder reaches ACE (terminal sentinel)");
    }

    @Test
    void pushAndEliminates_trueOnlyForAce() {
        Scoreboard sb = new Scoreboard();
        // Empty stack → pushAndEliminates returns false for non-ACE.
        assertFalse(sb.pushAndEliminates(new Card(Card.Suit.HEARTS, Card.Rank.SEVEN)));
        assertFalse(sb.pushAndEliminates(new Card(Card.Suit.HEARTS, Card.Rank.KING)));
        // Pushing ACE completes the ladder → eliminates.
        assertTrue(sb.pushAndEliminates(new Card(Card.Suit.HEARTS, Card.Rank.ACE)));
    }

    @Test
    void pushAdvancesLadder_headIsNewest() {
        Scoreboard sb = new Scoreboard();
        sb.pushAndEliminates(new Card(Card.Suit.CLUBS, Card.Rank.SEVEN));
        sb.pushAndEliminates(new Card(Card.Suit.CLUBS, Card.Rank.EIGHT));
        List<Card> stack = sb.snapshot().get(Card.Suit.CLUBS);
        assertEquals(2, stack.size());
        // Peek (top) is the most recently pushed card.
        assertEquals(Card.Rank.EIGHT, stack.get(0).rank());
    }
}
