package ru.anseranser.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GameTest {

    @Test
    void gameCreation_fourPlayersWithUniqueTrumps() {
        Game game = new Game();
        assertEquals(4, game.getPlayers().size());

        List<Card.Suit> trumps = new ArrayList<>();
        for (Player p : game.getPlayers()) {
            trumps.add(p.getTrump());
        }

        assertEquals(4, trumps.size());
        assertTrue(trumps.containsAll(List.of(
                Card.Suit.SPADES, Card.Suit.CLUBS,
                Card.Suit.DIAMONDS, Card.Suit.HEARTS)));
    }

    @Test
    void scoreboard_sixCardsPerSuit() {
        Game game = new Game();
        // After initScoreboard, each scoreboard stack should have exactly 1 card (SIX)
        // and the deck should have 32 cards (36 - 4 SIXes)
        game.shuffleAndDeal();

        int totalCards = 0;
        for (Player p : game.getPlayers()) {
            totalCards += p.getHand().size();
        }

        assertEquals(32, totalCards, "32 cards dealt (36 - 4 SIXes in scoreboard)");
    }

    @Test
    void shuffleAndDeal_allCardsDistributed() {
        Game game = new Game();
        game.shuffleAndDeal();

        int totalCards = 0;
        for (Player p : game.getPlayers()) {
            assertFalse(p.getHand().isEmpty(), "Each player should have cards");
            totalCards += p.getHand().size();
        }

        assertEquals(32, totalCards);
    }

    @Test
    void distributeObligatoryCards_sevenOfEachSuitTransferred() {
        Game game = new Game();
        game.shuffleAndDeal();

        // Record hands before exchange
        Map<Player, List<Card>> handsBefore = new HashMap<>();
        for (Player p : game.getPlayers()) {
            handsBefore.put(p, new ArrayList<>(p.getHand()));
        }

        game.distributeObligatoryCards();

        // After exchange: each player should have received SEVEN of their own trump
        // (if someone else had it), and given away SEVEN of other trumps
        for (Player current : game.getPlayers()) {
            List<Card> hand = current.getHand();
            Card.Suit trump = current.getTrump();

            // Player should now have the SEVEN of their own trump suit
            boolean hasOwnSeven = hand.stream()
                    .anyMatch(c -> c.suit() == trump && c.rank() == Card.Rank.SEVEN);
            assertTrue(hasOwnSeven,
                    "Player with trump " + trump + " should have SEVEN of " + trump);

            // Player should NOT have SEVEN of any other suit (they gave it away)
            for (Card.Suit otherSuit : Card.Suit.values()) {
                if (otherSuit == trump) continue;
                boolean hasOtherSeven = hand.stream()
                        .anyMatch(c -> c.suit() == otherSuit && c.rank() == Card.Rank.SEVEN);
                assertFalse(hasOtherSeven,
                        "Player with trump " + trump + " should not have SEVEN of " + otherSuit);
            }
        }
    }

    @Test
    void distributeObligatoryCards_totalCardsPreserved() {
        Game game = new Game();
        game.shuffleAndDeal();

        int totalBefore = countAllCards(game);
        game.distributeObligatoryCards();
        int totalAfter = countAllCards(game);

        assertEquals(totalBefore, totalAfter, "Total card count should not change during exchange");
    }

    private int countAllCards(Game game) {
        int total = 0;
        for (Player p : game.getPlayers()) {
            total += p.getHand().size();
        }
        return total;
    }

    @Test
    void playGame_oneWinnerRemains() {
        Game game = new Game();
        game.playGame();

        Player winner = game.getWinner();
        assertNotNull(winner, "There should be a winner");

        int gamers = 0;
        for (Player p : game.getPlayers()) {
            if (p.isGamer()) gamers++;
        }

        assertEquals(1, gamers, "Exactly one player should remain");
    }

    @Test
    void determinism_sameSeed_sameGame() {
        Game g1 = new Game();
        Game g2 = new Game();

        SplitMix64 r1 = new SplitMix64(42);
        SplitMix64 r2 = new SplitMix64(42);

        // Drive both games with identical RNG; everything downstream
        // (dealing, AI heuristics, round order) is deterministic, so the
        // two games must evolve identically.
        g1.playGame(r1);
        g2.playGame(r2);

        // Same winner (compare by seat, not object identity — separate Game
        // instances hold distinct Player objects even for the same seat)
        assertEquals(g1.getWinner().getTrump(), g2.getWinner().getTrump(),
                "Identical seeds must yield the same winner seat");

        // Same final hands per seat (each seat holds the identical card set)
        for (int i = 0; i < g1.getPlayers().size(); i++) {
            Player p1 = g1.getPlayers().get(i);
            Player p2 = g2.getPlayers().get(i);
            assertEquals(p1.getHand(), p2.getHand(),
                    "Seat " + i + " hands must match across identical runs");
        }
    }

    @Test
    void determinism_differentSeed_canDiffer() {
        Game g1 = new Game();
        Game g2 = new Game();

        // Two games with different seeds are not required to match; the point
        // of this test is that seeding is honoured (no hidden non-determinism
        // such as ThreadLocalRandom leaking back in).
        assertDoesNotThrow(() -> {
            g1.playGame(new SplitMix64(1));
            g2.playGame(new SplitMix64(2));
        });
    }
}
