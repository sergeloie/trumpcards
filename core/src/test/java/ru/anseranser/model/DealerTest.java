package ru.anseranser.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Dealer} — extracted from {@code Game} in Stage 4.
 * Verifies deck composition, deterministic shuffle under a seed, and that
 * dealing distributes exactly the 32 non-scoreboard cards among active players.
 */
class DealerTest {

    @Test
    void deckHasThirtySixUniqueCards() {
        Dealer dealer = new Dealer();
        assertEquals(36, dealer.deck().size());
        long distinct = dealer.deck().stream().distinct().count();
        assertEquals(36, distinct, "All 36 cards must be distinct");
    }

    @Test
    void sameSeed_producesSameShuffleOrder() {
        Dealer d1 = new Dealer();
        Dealer d2 = new Dealer();
        d1.shuffle(new SplitMix64(123));
        d2.shuffle(new SplitMix64(123));

        assertEquals(d1.deck(), d2.deck(),
                "Identical seeds must yield identical shuffle order");
    }

    @Test
    void deal_distributesAllNonScoreboardCardsToActivePlayers() {
        // Build a game-like setup: 4 players, all active gamers.
        List<Player> order = new ArrayList<>();
        for (Card.Suit suit : Card.Suit.values()) {
            order.add(new Player(suit));
        }
        TurnOrder turnOrder = new TurnOrder(order);

        Dealer dealer = new Dealer();
        Scoreboard sb = new Scoreboard();
        sb.init(dealer.deck()); // removes 4 SIXes → 32 cards left

        dealer.shuffle(new SplitMix64(7));
        dealer.deal(turnOrder, turnOrder.get(0), Player::isGamer);

        int total = 0;
        for (Player p : turnOrder) {
            assertFalse(p.getHand().isEmpty(), "Active player should receive cards");
            total += p.getHand().size();
        }
        assertEquals(32, total, "All 32 non-scoreboard cards must be dealt");
    }

    @Test
    void deal_skipsInactivePlayers() {
        List<Player> order = new ArrayList<>();
        for (Card.Suit suit : Card.Suit.values()) {
            order.add(new Player(suit));
        }
        TurnOrder turnOrder = new TurnOrder(order);
        // Mark two players as no longer in the game.
        order.get(1).setGamer(false);
        order.get(3).setGamer(false);

        Dealer dealer = new Dealer();
        Scoreboard sb = new Scoreboard();
        sb.init(dealer.deck());

        dealer.shuffle(new SplitMix64(7));
        dealer.deal(turnOrder, turnOrder.get(0), Player::isGamer);

        // Inactive players receive nothing; active ones split the 32 cards.
        assertEquals(0, order.get(1).getHand().size());
        assertEquals(0, order.get(3).getHand().size());

        int activeTotal = order.get(0).getHand().size() + order.get(2).getHand().size();
        assertEquals(32, activeTotal);
    }
}
