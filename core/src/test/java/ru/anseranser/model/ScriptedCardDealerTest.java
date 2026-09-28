package ru.anseranser.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link ScriptedCardDealer}: a deterministic, RNG-free
 * {@link CardDealer} that places an explicit layout into the players' hands.
 * Covers both valid layouts (exact dealing, interaction with
 * {@link Game#distributeObligatoryCards()}) and the eager validation of invalid
 * layouts (duplicates, wrong total, scoreboard cards, out-of-deck cards).
 */
class ScriptedCardDealerTest {

    /** A valid 36-card layout: the whole deck minus the four SIXes, dealt round-robin. */
    private static Map<Card.Suit, List<Card>> fullLayout36() {
        List<Card> deck = new ArrayList<>();
        for (Card.Suit suit : Card.Suit.values()) {
            for (Card.Rank rank : DeckSize.THIRTY_SIX.ranks()) {
                if (rank != DeckSize.THIRTY_SIX.baseRank()) {
                    deck.add(new Card(suit, rank));
                }
            }
        }
        Map<Card.Suit, List<Card>> hands = new EnumMap<>(Card.Suit.class);
        for (Card.Suit suit : Card.Suit.values()) {
            hands.put(suit, new ArrayList<>());
        }
        for (int i = 0; i < deck.size(); i++) {
            hands.get(Card.Suit.values()[i % Card.Suit.values().length]).add(deck.get(i));
        }
        return hands;
    }

    private static int totalHandCards(Game game) {
        int total = 0;
        for (Player p : game.getPlayers()) {
            total += p.getHand().size();
        }
        return total;
    }

    @Test
    void deal_placesExactlyTheScriptedCardsInEachHand() {
        Map<Card.Suit, List<Card>> layout = fullLayout36();
        Game game = new Game(DeckSize.THIRTY_SIX);
        game.setCardDealer(new ScriptedCardDealer(DeckSize.THIRTY_SIX, layout));

        game.shuffleAndDeal();

        for (Player p : game.getPlayers()) {
            assertEquals(layout.get(p.getTrump()), p.getHand(),
                    "Player with trump " + p.getTrump() + " must receive exactly the scripted hand");
        }
        assertEquals(32, totalHandCards(game), "All 32 non-scoreboard cards must be dealt");
    }

    @Test
    void injectedDealerIsUsedByGame_andRngIsIgnored() {
        Map<Card.Suit, List<Card>> layout = fullLayout36();
        Game a = new Game(DeckSize.THIRTY_SIX);
        Game b = new Game(DeckSize.THIRTY_SIX);
        a.setCardDealer(new ScriptedCardDealer(DeckSize.THIRTY_SIX, layout));
        b.setCardDealer(new ScriptedCardDealer(DeckSize.THIRTY_SIX, layout));
        // Different RNGs must not matter when the dealer is scripted.
        a.setRng(new SplitMix64(1));
        b.setRng(new SplitMix64(999));

        a.shuffleAndDeal();
        b.shuffleAndDeal();

        for (int i = 0; i < a.getPlayers().size(); i++) {
            assertEquals(a.getPlayers().get(i).getHand(), b.getPlayers().get(i).getHand(),
                    "Scripted dealing must be independent of the RNG seed");
        }
    }

    @Test
    void distributeObligatoryCards_runsOverScriptedLayout() {
        Map<Card.Suit, List<Card>> layout = fullLayout36();
        Game game = new Game(DeckSize.THIRTY_SIX);
        game.setCardDealer(new ScriptedCardDealer(DeckSize.THIRTY_SIX, layout));
        game.shuffleAndDeal();

        game.distributeObligatoryCards();

        // Card conservation: the exchange only moves cards between hands.
        assertEquals(32, totalHandCards(game), "Obligatory exchange must not change the card total");
        // After the exchange every player holds the SEVEN of their own trump.
        for (Player p : game.getPlayers()) {
            Card.Suit trump = p.getTrump();
            boolean hasOwnSeven = p.getHand().stream()
                    .anyMatch(c -> c.suit() == trump && c.rank() == Card.Rank.SEVEN);
            assertTrue(hasOwnSeven,
                    "Player with trump " + trump + " should hold the SEVEN of " + trump + " after the exchange");
        }
    }

    @Test
    void takeBaseCards_returnsOneBaseCardPerSuit() {
        ScriptedCardDealer dealer = new ScriptedCardDealer(DeckSize.THIRTY_SIX, fullLayout36());
        List<Card> base = dealer.takeBaseCards(DeckSize.THIRTY_SIX.baseRank());
        assertEquals(4, base.size());
        for (Card.Suit suit : Card.Suit.values()) {
            assertTrue(base.contains(new Card(suit, Card.Rank.SIX)),
                    "Missing base SIX of " + suit);
        }
    }

    @Test
    void duplicateCard_isRejected() {
        Card dup = new Card(Card.Suit.SPADES, Card.Rank.SEVEN);
        Map<Card.Suit, List<Card>> layout = new EnumMap<>(Card.Suit.class);
        layout.put(Card.Suit.SPADES, List.of(dup, dup));
        assertThrows(IllegalArgumentException.class,
                () -> new ScriptedCardDealer(DeckSize.THIRTY_SIX, layout));
    }

    @Test
    void wrongTotal_isRejected() {
        Map<Card.Suit, List<Card>> layout = fullLayout36();
        // Drop one card so the layout no longer covers the whole non-scoreboard deck.
        layout.get(Card.Suit.SPADES).remove(0);
        assertThrows(IllegalArgumentException.class,
                () -> new ScriptedCardDealer(DeckSize.THIRTY_SIX, layout));
    }

    @Test
    void scoreboardBaseCardInHand_isRejected() {
        Map<Card.Suit, List<Card>> layout = new EnumMap<>(Card.Suit.class);
        layout.put(Card.Suit.SPADES, List.of(new Card(Card.Suit.SPADES, Card.Rank.SIX)));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new ScriptedCardDealer(DeckSize.THIRTY_SIX, layout));
        assertTrue(ex.getMessage().contains("scoreboard"), "Error should explain the scoreboard invariant");
    }

    @Test
    void cardFromOtherDeckSize_isRejected() {
        // TWO is not part of a 36-card deck (its lowest rank is SIX).
        Map<Card.Suit, List<Card>> layout = new EnumMap<>(Card.Suit.class);
        layout.put(Card.Suit.SPADES, List.of(new Card(Card.Suit.SPADES, Card.Rank.TWO)));
        assertThrows(IllegalArgumentException.class,
                () -> new ScriptedCardDealer(DeckSize.THIRTY_SIX, layout));
    }

    @Test
    void scriptMissingAPlayer_isRejectedAtDeal() {
        // A structurally valid 32-card layout, but keyed for only 3 players.
        Map<Card.Suit, List<Card>> layout = fullLayout36();
        Map<Card.Suit, List<Card>> threeHands = new EnumMap<>(Card.Suit.class);
        threeHands.put(Card.Suit.SPADES, new ArrayList<>(layout.get(Card.Suit.SPADES)));
        threeHands.put(Card.Suit.CLUBS, new ArrayList<>(layout.get(Card.Suit.CLUBS)));
        threeHands.put(Card.Suit.DIAMONDS, new ArrayList<>(layout.get(Card.Suit.DIAMONDS)));
        threeHands.get(Card.Suit.SPADES).addAll(layout.get(Card.Suit.HEARTS)); // keep the 32-card total valid

        Game game = new Game(DeckSize.THIRTY_SIX);
        game.setCardDealer(new ScriptedCardDealer(DeckSize.THIRTY_SIX, threeHands));

        assertThrows(IllegalArgumentException.class, game::shuffleAndDeal);
    }

    // ---------- Layout builder ----------

    @Test
    void builder_pinsExplicitCards_andFillsTheRest() {
        Card sevenSpades = new Card(Card.Suit.SPADES, Card.Rank.SEVEN);
        Card nineClubs = new Card(Card.Suit.CLUBS, Card.Rank.NINE);
        Card aceHearts = new Card(Card.Suit.HEARTS, Card.Rank.ACE);

        ScriptedCardDealer dealer = ScriptedCardDealer.builder(DeckSize.THIRTY_SIX)
                .give(Card.Suit.SPADES, sevenSpades, nineClubs)
                .give(Card.Suit.HEARTS, aceHearts)
                .buildDealer();

        Game game = new Game(DeckSize.THIRTY_SIX);
        game.setCardDealer(dealer);
        game.shuffleAndDeal();

        List<Card> spades = game.getPlayers().get(0).getHand();
        assertEquals(List.of(sevenSpades, nineClubs), spades.subList(0, 2),
                "Pinned cards must appear at the head of the owner's hand");
        List<Card> hearts = game.getPlayers().get(3).getHand();
        assertEquals(aceHearts, hearts.get(0));

        assertEquals(32, totalHandCards(game), "The rest of the deck must be auto-filled");
        List<Card> dealtCards = new ArrayList<>();
        for (Player p : game.getPlayers()) {
            dealtCards.addAll(p.getHand());
        }
        assertEquals(32, dealtCards.stream().distinct().count(),
                "Auto-fill must not duplicate a pinned card");
    }

    @Test
    void builder_duplicateCard_isRejected() {
        Card dup = new Card(Card.Suit.SPADES, Card.Rank.SEVEN);
        assertThrows(IllegalArgumentException.class, () -> ScriptedCardDealer.builder(DeckSize.THIRTY_SIX)
                .give(Card.Suit.SPADES, dup)
                .give(Card.Suit.HEARTS, dup));
    }

    @Test
    void builder_scoreboardBaseCard_isRejected() {
        assertThrows(IllegalArgumentException.class, () -> ScriptedCardDealer.builder(DeckSize.THIRTY_SIX)
                .give(Card.Suit.SPADES, new Card(Card.Suit.SPADES, Card.Rank.SIX)));
    }

    @Test
    void builder_outOfDeckCard_isRejected() {
        assertThrows(IllegalArgumentException.class, () -> ScriptedCardDealer.builder(DeckSize.THIRTY_SIX)
                .give(Card.Suit.SPADES, new Card(Card.Suit.SPADES, Card.Rank.TWO)));
    }

    @Test
    void builder_fillIsIdempotent() {
        Map<Card.Suit, List<Card>> layout = ScriptedCardDealer.builder(DeckSize.THIRTY_SIX)
                .give(Card.Suit.CLUBS, new Card(Card.Suit.CLUBS, Card.Rank.KING))
                .fill()
                .fill()
                .build();
        int total = layout.values().stream().mapToInt(List::size).sum();
        assertEquals(32, total, "Calling fill() twice must not duplicate cards");
        long distinct = layout.values().stream().flatMap(List::stream).distinct().count();
        assertEquals(32, distinct);
    }

    @Test
    void full_dealer_dealsBalancedHands() {
        Game game = new Game(DeckSize.THIRTY_SIX);
        game.setCardDealer(ScriptedCardDealer.full(DeckSize.THIRTY_SIX));
        game.shuffleAndDeal();
        for (Player p : game.getPlayers()) {
            assertEquals(8, p.getHand().size(), "32 cards split evenly across 4 players");
        }
    }

    @Test
    void builder_fillOrderControlsAutoFill() {
        // Only HEARTS and SPADES receive the auto-filled cards, in that order.
        ScriptedCardDealer dealer = ScriptedCardDealer.builder(DeckSize.THIRTY_SIX)
                .fillOrder(Card.Suit.HEARTS, Card.Suit.SPADES)
                .buildDealer();
        Game game = new Game(DeckSize.THIRTY_SIX);
        game.setCardDealer(dealer);
        game.shuffleAndDeal();

        // Default player order is SPADES(0), CLUBS(1), DIAMONDS(2), HEARTS(3).
        assertEquals(16, game.getPlayers().get(3).getHand().size(), "HEARTS first in the fill order");
        assertEquals(16, game.getPlayers().get(0).getHand().size(), "SPADES second in the fill order");
        assertEquals(0, game.getPlayers().get(1).getHand().size(), "CLUBS not in the fill order");
        assertEquals(0, game.getPlayers().get(2).getHand().size(), "DIAMONDS not in the fill order");
    }

    @Test
    void builder_fillOrderIsDeterministicAcrossRuns() {
        Map<Card.Suit, List<Card>> first = ScriptedCardDealer.builder(DeckSize.THIRTY_SIX)
                .fillOrder(Card.Suit.DIAMONDS, Card.Suit.HEARTS, Card.Suit.CLUBS, Card.Suit.SPADES)
                .build();
        Map<Card.Suit, List<Card>> second = ScriptedCardDealer.builder(DeckSize.THIRTY_SIX)
                .fillOrder(Card.Suit.DIAMONDS, Card.Suit.HEARTS, Card.Suit.CLUBS, Card.Suit.SPADES)
                .build();
        assertEquals(first, second, "Identical builder config must yield identical layouts");
    }

    @Test
    void full_withExplicitOrder_dealsInThatCycle() {
        Game game = new Game(DeckSize.THIRTY_SIX);
        game.setCardDealer(ScriptedCardDealer.full(DeckSize.THIRTY_SIX,
                Card.Suit.CLUBS, Card.Suit.DIAMONDS, Card.Suit.HEARTS, Card.Suit.SPADES));
        game.shuffleAndDeal();
        for (Player p : game.getPlayers()) {
            assertEquals(8, p.getHand().size(), "All four suits are in the cycle, so hands stay even");
        }
    }

    @Test
    void builder_emptyFillOrder_isRejected() {
        assertThrows(IllegalArgumentException.class, () -> ScriptedCardDealer.builder(DeckSize.THIRTY_SIX)
                .fillOrder());
    }
}
