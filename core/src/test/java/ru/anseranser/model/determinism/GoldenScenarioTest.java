package ru.anseranser.model.determinism;

import org.junit.jupiter.api.Test;
import ru.anseranser.event.GameEvent;
import ru.anseranser.model.AiDecisionStrategy;
import ru.anseranser.model.Card;
import ru.anseranser.model.DeckSize;
import ru.anseranser.model.Game;
import ru.anseranser.model.Player;
import ru.anseranser.model.ScriptedCardDealer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Golden/scenario tests: hand-written layouts dealt by {@link ScriptedCardDealer}
 * with all randomness removed (fixed first dealer via {@link FixedRng}, AI
 * decisions in {@link AiDecisionStrategy}). Each scenario pins down an outcome
 * that {@code rules.md} specifies, so any future change to the engine that alters
 * game logic is caught here.
 */
class GoldenScenarioTest {

    private static final Card.Rank BASE = DeckSize.THIRTY_SIX.baseRank();

    /** Drive a game to completion, collecting every event. */
    private static List<GameEvent> runFull(Game game) {
        List<GameEvent> events = new ArrayList<>();
        game.setListener(events::add);
        Game.GameDriver d = game.createDriver();
        d.startGame();
        while (!d.isGameOver()) {
            while (d.step()) {
                // one move per step
            }
            d.finishRound();
        }
        return events;
    }

    private static <T extends GameEvent> List<T> ofType(List<GameEvent> events, Class<T> type) {
        List<T> out = new ArrayList<>();
        for (GameEvent e : events) {
            if (type.isInstance(e)) {
                out.add(type.cast(e));
            }
        }
        return out;
    }

    // ---------- Scenario 1: obligatory card exchange (rules.md §2.3) ----------

    @Test
    void obligatoryExchange_movesEverySeventhToItsSuitOwner() {
        // Give each suit's SEVEN to a DIFFERENT player, so every seat must hand it over.
        Map<Card.Suit, List<Card>> layout = ScriptedCardDealer.builder(DeckSize.THIRTY_SIX)
                .give(Card.Suit.SPADES, new Card(Card.Suit.CLUBS, Card.Rank.SEVEN))
                .give(Card.Suit.CLUBS, new Card(Card.Suit.DIAMONDS, Card.Rank.SEVEN))
                .give(Card.Suit.DIAMONDS, new Card(Card.Suit.HEARTS, Card.Rank.SEVEN))
                .give(Card.Suit.HEARTS, new Card(Card.Suit.SPADES, Card.Rank.SEVEN))
                .build();

        Game game = new Game(DeckSize.THIRTY_SIX);
        game.setCardDealer(new ScriptedCardDealer(DeckSize.THIRTY_SIX, layout));
        game.shuffleAndDeal();
        game.distributeObligatoryCards();

        for (Player p : game.getPlayers()) {
            Card ownSeven = new Card(p.getTrump(), Card.Rank.SEVEN);
            assertTrue(p.getHand().contains(ownSeven),
                    "Player " + p.getTrump() + " must hold the SEVEN of their own suit after the exchange");
            for (Card.Suit other : Card.Suit.values()) {
                if (other != p.getTrump()) {
                    assertFalse(p.getHand().contains(new Card(other, Card.Rank.SEVEN)),
                            "The SEVEN of " + other + " must have been handed to its owner");
                }
            }
        }
    }

    // ---------- Scenario 2: defender with no legal defense takes the pot (rules.md §3.4) ----------

    @Test
    void defenderWithNoLegalDefense_takesThePot() {
        Card clubsEight = new Card(Card.Suit.CLUBS, Card.Rank.EIGHT);
        Card clubsSeven = new Card(Card.Suit.CLUBS, Card.Rank.SEVEN);

        // CLUBS gets no handwritten card above the EIGHT, and fill() never targets CLUBS,
        // so it cannot beat CLUBS EIGHT (same suit needs a higher rank; its trump is CLUBS).
        Map<Card.Suit, List<Card>> layout = ScriptedCardDealer.builder(DeckSize.THIRTY_SIX)
                .give(Card.Suit.SPADES, clubsEight)
                .give(Card.Suit.CLUBS, clubsSeven)
                .fillOrder(Card.Suit.SPADES, Card.Suit.DIAMONDS, Card.Suit.HEARTS)
                .build();

        List<Player> players = new ArrayList<>();
        players.add(new Player(Card.Suit.SPADES, new LeadCardStrategy(clubsEight)));
        players.add(new Player(Card.Suit.CLUBS, new AiDecisionStrategy()));
        players.add(new Player(Card.Suit.DIAMONDS, new AiDecisionStrategy()));
        players.add(new Player(Card.Suit.HEARTS, new AiDecisionStrategy()));

        Game game = new Game(players, DeckSize.THIRTY_SIX);
        game.setCardDealer(new ScriptedCardDealer(DeckSize.THIRTY_SIX, layout));
        game.setRng(new FixedRng(0)); // first dealer = seat 0 (SPADES)

        List<GameEvent> events = new ArrayList<>();
        game.setListener(events::add);
        Game.GameDriver d = game.createDriver();
        d.startGame();
        d.step(); // SPADES leads CLUBS EIGHT
        d.step(); // CLUBS cannot beat → takes the pot

        List<GameEvent.CardPlayed> played = ofType(events, GameEvent.CardPlayed.class);
        List<GameEvent.PotTaken> taken = ofType(events, GameEvent.PotTaken.class);

        assertEquals(1, played.size());
        assertEquals(Card.Suit.SPADES, played.get(0).player());
        assertEquals(clubsEight, played.get(0).card());

        assertEquals(1, taken.size());
        assertEquals(Card.Suit.CLUBS, taken.get(0).player(), "The player to the leader's left must take the pot");
        assertEquals(clubsEight, taken.get(0).topCard());
        assertEquals(1, taken.get(0).potSize());
    }

    // ---------- Scenario 3: a successful beat hands the lead to the same player (rules.md §3.3) ----------

    @Test
    void successfulBeat_letsTheDefenderLeadTheNextTrick() {
        Card clubsEight = new Card(Card.Suit.CLUBS, Card.Rank.EIGHT);
        Card clubsNine = new Card(Card.Suit.CLUBS, Card.Rank.NINE);

        Map<Card.Suit, List<Card>> layout = ScriptedCardDealer.builder(DeckSize.THIRTY_SIX)
                .give(Card.Suit.SPADES, clubsEight)
                .give(Card.Suit.CLUBS, clubsNine)
                .fillOrder(Card.Suit.SPADES, Card.Suit.DIAMONDS, Card.Suit.HEARTS)
                .build();

        List<Player> players = new ArrayList<>();
        players.add(new Player(Card.Suit.SPADES, new LeadCardStrategy(clubsEight)));
        players.add(new Player(Card.Suit.CLUBS, new AiDecisionStrategy()));
        players.add(new Player(Card.Suit.DIAMONDS, new AiDecisionStrategy()));
        players.add(new Player(Card.Suit.HEARTS, new AiDecisionStrategy()));

        Game game = new Game(players, DeckSize.THIRTY_SIX);
        game.setCardDealer(new ScriptedCardDealer(DeckSize.THIRTY_SIX, layout));
        game.setRng(new FixedRng(0));

        List<GameEvent> events = new ArrayList<>();
        game.setListener(events::add);
        Game.GameDriver d = game.createDriver();
        d.startGame();
        d.step(); // SPADES leads CLUBS EIGHT
        d.step(); // CLUBS beats with CLUBS NINE, then leads

        List<GameEvent.CardBeaten> beaten = ofType(events, GameEvent.CardBeaten.class);
        assertEquals(1, beaten.size());
        assertEquals(Card.Suit.CLUBS, beaten.get(0).player());
        assertEquals(clubsEight, beaten.get(0).attacking());
        assertEquals(clubsNine, beaten.get(0).beating());

        // After beating, the same player must lead the next trick.
        List<GameEvent.CardPlayed> played = ofType(events, GameEvent.CardPlayed.class);
        assertEquals(2, played.size());
        assertEquals(Card.Suit.CLUBS, played.get(1).player(),
                "The successful defender leads the next trick");
    }

    // ---------- Scenario 4: round end pushes the next trump (rules.md §6) ----------

    @Test
    void roundEnd_pushesNextTrumpToLoserScoreboard() {
        Game game = new Game(DeckSize.THIRTY_SIX);
        game.setCardDealer(ScriptedCardDealer.full(DeckSize.THIRTY_SIX));
        game.setRng(new FixedRng(0));

        List<GameEvent> events = new ArrayList<>();
        game.setListener(events::add);
        Game.GameDriver d = game.createDriver();
        d.startGame();
        while (d.step()) {
            // one round only
        }
        d.finishRound();

        GameEvent.RoundEnded ended = ofType(events, GameEvent.RoundEnded.class).get(0);
        Card pushed = ended.pushedToScoreboard();
        assertNotNull(pushed, "First round: the loser still holds trumps, so a card is pushed");
        assertEquals(ended.loser(), pushed.suit(), "The pushed card belongs to the loser's trump suit");
        assertEquals(Card.Rank.SEVEN, pushed.rank(),
                "After the base " + BASE + " the ladder's next required card is SEVEN");
        assertFalse(ended.eliminated(), "Pushing a SEVEN does not complete the ladder");
        assertTrue(game.getScoreboard().get(ended.loser()).contains(pushed),
                "The pushed card must appear on the loser's scoreboard ladder");
    }

    // ---------- Scenario 5: elimination when the ladder reaches ACE (rules.md §4, §6) ----------

    @Test
    void fullScriptedGame_completesLadderToAce_andEndsWithOneWinner() {
        Game game = new Game(DeckSize.THIRTY_SIX);
        game.setCardDealer(ScriptedCardDealer.full(DeckSize.THIRTY_SIX));
        game.setRng(new FixedRng(0));

        List<GameEvent> events = runFull(game);

        List<GameEvent.RoundEnded> rounds = ofType(events, GameEvent.RoundEnded.class);
        GameEvent.RoundEnded elimination = rounds.stream()
                .filter(GameEvent.RoundEnded::eliminated)
                .findFirst()
                .orElseThrow(() -> new AssertionError("expected at least one ladder-completing (ACE) elimination"));
        assertNotNull(elimination.pushedToScoreboard());
        assertEquals(Card.Rank.ACE, elimination.pushedToScoreboard().rank(),
                "Elimination happens exactly when an ACE is pushed");

        List<GameEvent.GameEnded> gameEnded = ofType(events, GameEvent.GameEnded.class);
        assertEquals(1, gameEnded.size(), "GameEnded must fire exactly once");
        assertNotNull(game.getWinner());
        assertEquals(game.getWinner().getTrump(), gameEnded.get(0).winner(),
                "The GameEnded winner must be the surviving gamer");
        // Frozen golden outcome: with the full round-robin layout and a fixed first dealer,
        // this exact scripted game is won by HEARTS. Any engine change that alters the
        // rules will break this assertion and must be reviewed deliberately.
        assertEquals(Card.Suit.HEARTS, game.getWinner().getTrump(),
                "Golden winner for the full scripted game");

        int gamers = 0;
        for (Player p : game.getPlayers()) {
            if (p.isGamer()) {
                gamers++;
            }
        }
        assertEquals(1, gamers, "Exactly one player remains a gamer");
    }
}
