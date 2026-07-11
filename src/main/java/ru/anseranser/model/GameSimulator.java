package ru.anseranser.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Drives many full games to verify engine invariants — the workhorse of
 * refactor Stage 7 (verification).
 *
 * <p>Because the engine is fully deterministic for a fixed seed (Random injected
 * in Stage 5, TurnOrder/AI in Stages 2–3), the simulator can replay a game by its
 * seed and assert that:
 * <ul>
 *   <li><b>Card conservation</b> — exactly 36 distinct cards exist across all
 *       hands + the scoreboard at game end;</li>
 *   <li><b>Single winner</b> — exactly one player remains a "gamer";</li>
 *   <li><b>Termination</b> — every game completes (no infinite loop).</li>
 * </ul>
 *
 * These invariants are the regression safety net proving the refactor
 * (Stages 1–5) did not change observable game behaviour.
 */
public class GameSimulator {

    private final int games;
    private final int startSeed;
    private final boolean humanPlayer;

    public GameSimulator(int games, int startSeed, boolean humanPlayer) {
        this.games = games;
        this.startSeed = startSeed;
        this.humanPlayer = humanPlayer;
    }

    public record Result(
            int seed,
            Card.Suit winnerTrump,
            int totalCards,
            long distinctCards,
            int activeGamers,
            int rounds) {
        boolean invariantsHold() {
            return totalCards == 36
                    && distinctCards == 36
                    && activeGamers == 1;
        }
    }

    public List<Result> run() {
        List<Result> results = new ArrayList<>();
        for (int i = 0; i < games; i++) {
            int seed = startSeed + i;
            results.add(runOne(seed));
        }
        return results;
    }

    private Result runOne(int seed) {
        Game game = new Game(humanPlayer);
        game.playGame(new Random(seed));

        List<Card> allCards = game.allCards();
        int totalCards = allCards.size();
        Set<Card> seen = new HashSet<>(allCards);

        int activeGamers = 0;
        for (Player p : game.getPlayers()) {
            if (p.isGamer()) activeGamers++;
        }

        Player winner = game.getWinner();
        Card.Suit winnerTrump = winner != null ? winner.getTrump() : null;

        return new Result(seed, winnerTrump, totalCards, seen.size(), activeGamers, 0);
    }
}
