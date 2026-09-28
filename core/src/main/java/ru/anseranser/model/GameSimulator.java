package ru.anseranser.model;

import ru.anseranser.event.GameEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
 *   <li><b>Termination</b> — every game completes (no infinite loop), and
 *       ideally without tripping the defensive {@code MAX_ROUND_MOVES} cap;</li>
 *   <li><b>Determinism</b> — replaying the same seed yields the same winner.</li>
 * </ul>
 *
 * <p>The {@link Result} also reports how many of the simulated games actually
 * hit the safety cap ({@link #countCapped}), so the suite can surface — rather
 * than hide — the pathological non-terminating deals that the cap catches.</p>
 *
 * <p>{@link #replay(int)} additionally captures the <em>whole path</em> of a
 * game: a {@link RoundSnapshot} per round (loser, pushed card, elimination flag,
 * move count). Two independent replays of the same seed must produce identical
 * paths — this is the round-level determinism guarantee (golden/replay tests).</p>
 */
public class GameSimulator {

    private final int games;
    private final int startSeed;
    private final DecisionStrategy humanStrategy;
    private final DeckSize deckSize;

    /**
     * @param humanStrategy strategy for the SPADES seat (pass {@code null} for a
     *                      fully automated game). Injected rather than a boolean
     *                      flag so the domain stays free of any input dependency (R6/R8).
     */
    public GameSimulator(int games, int startSeed, DecisionStrategy humanStrategy) {
        this(games, startSeed, humanStrategy, DeckSize.THIRTY_SIX);
    }

    /** Overload that lets the caller pick the deck size (36 or 52 cards). */
    public GameSimulator(int games, int startSeed, DecisionStrategy humanStrategy, DeckSize deckSize) {
        this.games = games;
        this.startSeed = startSeed;
        this.humanStrategy = humanStrategy;
        this.deckSize = deckSize;
    }

    public record Result(
            int seed,
            Card.Suit winnerTrump,
            int totalCards,
            long distinctCards,
            int activeGamers,
            int rounds,
            int cappedRounds,
            int expectedCards) {
        boolean invariantsHold() {
            return totalCards == expectedCards
                    && distinctCards == expectedCards
                    && activeGamers == 1;
        }
    }

    /**
     * The outcome of a single round within a replayed game.
     *
     * @param round      1-based round index
     * @param loser      the round's loser (their trump suit)
     * @param pushed     the card pushed onto the loser's scoreboard ({@code null} if none)
     * @param eliminated whether the push completed the ladder and eliminated the loser
     * @param moves      how many moves (lead/beat/take) the round took
     */
    public record RoundSnapshot(int round, Card.Suit loser, Card pushed, boolean eliminated, int moves) {}

    /** A full replay of one seed: the final {@link Result} plus every round's {@link RoundSnapshot}. */
    public record Replay(Result result, List<RoundSnapshot> rounds) {}

    public List<Result> run() {
        List<Result> results = new ArrayList<>();
        for (int i = 0; i < games; i++) {
            int seed = startSeed + i;
            results.add(runOne(seed));
        }
        return results;
    }

    /** Counts how many of the supplied results hit the defensive move cap. */
    public static int countCapped(List<Result> results) {
        int capped = 0;
        for (Result r : results) {
            if (r.cappedRounds() > 0) capped++;
        }
        return capped;
    }

    private Result runOne(int seed) {
        Game game = new Game(createPlayers(), deckSize);
        game.playGame(new SplitMix64(seed));
        return resultOf(seed, game);
    }

    /**
     * Run one seed and capture both the final result and the per-round path.
     * Two calls with the same seed must return equal {@code Replay}s.
     */
    public Replay replay(int seed) {
        Game game = new Game(createPlayers(), deckSize);
        List<RoundSnapshot> rounds = new ArrayList<>();
        int[] moves = {0};
        game.setListener(event -> {
            if (event instanceof GameEvent.CardPlayed
                    || event instanceof GameEvent.CardBeaten
                    || event instanceof GameEvent.PotTaken) {
                moves[0]++;
            } else if (event instanceof GameEvent.RoundEnded e) {
                rounds.add(new RoundSnapshot(rounds.size() + 1, e.loser(),
                        e.pushedToScoreboard(), e.eliminated(), moves[0]));
                moves[0] = 0;
            }
        });
        game.playGame(new SplitMix64(seed));
        return new Replay(resultOf(seed, game), List.copyOf(rounds));
    }

    private List<Player> createPlayers() {
        List<Player> players = new ArrayList<>();
        for (Card.Suit suit : Card.Suit.values()) {
            DecisionStrategy s = (humanStrategy != null && suit == Card.Suit.SPADES)
                    ? humanStrategy : new AiDecisionStrategy();
            players.add(new Player(suit, s));
        }
        return players;
    }

    private Result resultOf(int seed, Game game) {
        List<Card> allCards = game.allCards();
        int totalCards = allCards.size();
        Set<Card> seen = new HashSet<>(allCards);

        int activeGamers = 0;
        for (Player p : game.getPlayers()) {
            if (p.isGamer()) activeGamers++;
        }

        Player winner = game.getWinner();
        Card.Suit winnerTrump = winner != null ? winner.getTrump() : null;

        return new Result(
                seed,
                winnerTrump,
                totalCards,
                seen.size(),
                activeGamers,
                game.getRoundsPlayed(),
                game.getCappedRounds(),
                deckSize.cardCount());
    }
}
