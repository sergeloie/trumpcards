package ru.anseranser.model;

import org.junit.jupiter.api.Test;

import ru.anseranser.model.DeckSize;
import ru.anseranser.model.Player;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies R10: a game saved mid-play can be serialized to JSON, loaded back,
 * and continued to completion — producing the same winner and conserving all
 * 36 cards, exactly as if it had never been interrupted.
 */
class GameSerializationTest {

    private static final long SEED = 42L;
    private static final int SAVE_AFTER_STEPS = 15;

    private static int totalCards(Game game) {
        return game.allCards().size() + game.getPot().size();
    }

    @Test
    void savedGameContinuesIdenticallyToUninterruptedGame() {
        // Reference: a full, uninterrupted game with a fixed seed.
        Game reference = new Game();
        reference.playGame(new SplitMix64(SEED));
        Card.Suit expectedWinner = reference.getWinner().getTrump();

        // Play the same game up to a save point, then capture state.
        Game game = new Game();
        game.setRng(new SplitMix64(SEED));
        Game.GameDriver driver = game.createDriver();
        driver.startGame();

        int steps = 0;
        while (!driver.isGameOver() && steps < SAVE_AFTER_STEPS) {
            if (!driver.step()) {
                driver.finishRound();
                continue;
            }
            steps++;
        }
        assertFalse(driver.isGameOver(), "save point should be mid-game for the test to be meaningful");
        assertEquals(36, totalCards(game), "card conservation broken at save point");

        SavedGame saved = driver.save();

        // Round-trip through JSON (the portable format platforms persist).
        String json = saved.toJson();
        SavedGame reloaded = SavedGame.fromJson(json);

        // Resume on a fresh engine instance.
        Game.GameDriver resumed = Game.restore(reloaded, suit -> new AiDecisionStrategy());
        while (!resumed.isGameOver()) {
            while (resumed.step()) {
                // render / animate between moves
            }
            resumed.finishRound();
        }

        assertEquals(expectedWinner, resumed.getGame().getWinner().getTrump(),
                "resumed game must finish with the same winner as the uninterrupted one");
        assertEquals(36, totalCards(resumed.getGame()), "card conservation broken after resume");
    }

    @Test
    void savedGameJsonIsStableAndReadable() {
        Game game = new Game();
        game.setRng(new SplitMix64(SEED));
        Game.GameDriver driver = game.createDriver();
        driver.startGame();
        for (int i = 0; i < 5 && !driver.isGameOver(); i++) {
            if (!driver.step()) driver.finishRound();
        }

        String json = driver.save().toJson();
        assertTrue(json.contains("\"version\":1"), "JSON should carry a version field");
        assertTrue(json.contains("SPADES") || json.contains("HEARTS")
                || json.contains("CLUBS") || json.contains("DIAMONDS"),
                "JSON should encode suits as readable strings");

        // Re-parsing must not throw and must round-trip the rng seed.
        SavedGame reloaded = SavedGame.fromJson(json);
        assertEquals(driver.save().rngSeed(), reloaded.rngSeed());
    }

    @Test
    void savedGameFiftyTwoDeck_roundTripsDeckSize() {
        // A 52-card game must serialize its deck size and resume identically.
        Game reference = new Game(defaultPlayers(), DeckSize.FIFTY_TWO);
        reference.playGame(new SplitMix64(SEED));
        Card.Suit expectedWinner = reference.getWinner().getTrump();

        Game game = new Game(defaultPlayers(), DeckSize.FIFTY_TWO);
        game.setRng(new SplitMix64(SEED));
        Game.GameDriver driver = game.createDriver();
        driver.startGame();
        int steps = 0;
        while (!driver.isGameOver() && steps < SAVE_AFTER_STEPS) {
            if (!driver.step()) {
                driver.finishRound();
                continue;
            }
            steps++;
        }
        assertFalse(driver.isGameOver(), "save point should be mid-game for the test to be meaningful");
        assertEquals(52, totalCards(game), "card conservation broken at save point (52-card deck)");

        SavedGame saved = driver.save();
        assertEquals(DeckSize.FIFTY_TWO, saved.deckSize(), "deck size must be persisted");

        String json = saved.toJson();
        SavedGame reloaded = SavedGame.fromJson(json);
        assertEquals(DeckSize.FIFTY_TWO, reloaded.deckSize(), "deck size must round-trip through JSON");

        Game.GameDriver resumed = Game.restore(reloaded, suit -> new AiDecisionStrategy());
        while (!resumed.isGameOver()) {
            while (resumed.step()) {
                // render / animate between moves
            }
            resumed.finishRound();
        }

        assertEquals(expectedWinner, resumed.getGame().getWinner().getTrump(),
                "resumed 52-card game must finish with the same winner");
        assertEquals(52, totalCards(resumed.getGame()), "card conservation broken after resume");
    }

    private static List<Player> defaultPlayers() {
        List<Player> order = new ArrayList<>();
        for (Card.Suit suit : Card.Suit.values()) {
            order.add(new Player(suit));
        }
        return order;
    }
}
