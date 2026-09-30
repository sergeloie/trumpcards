package ru.anseranser.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

/**
 * Per-round queries over the seating order (finding I1/I4b).
 *
 * <p>Every "which players …" question the round asks is expressed as a filter
 * over one walk of the {@link TurnOrder}, so the loops cannot drift apart in
 * what they visit or in what order. Kept package-private: this is an internal
 * detail of {@link Game}'s round orchestration, not part of the public model.
 *
 * <p>All methods preserve the seating order of the given {@link TurnOrder}, which
 * is what keeps the round deterministic and the tie-breaks stable.
 */
final class RoundResolver {

    private RoundResolver() {
    }

    /** Every player satisfying {@code predicate}, in seating order. */
    static List<Player> where(TurnOrder players, Predicate<Player> predicate) {
        List<Player> result = new ArrayList<>();
        for (Player p : players) {
            if (predicate.test(p)) {
                result.add(p);
            }
        }
        return result;
    }

    /** Players still in the round: active gamers holding at least one card, in seating order. */
    static List<Player> activeWithCards(TurnOrder players) {
        return where(players, p -> p.isGamer() && !p.getHand().isEmpty());
    }

    /** Number of players still in the game, regardless of whether they hold cards. */
    static int countActive(TurnOrder players) {
        return where(players, Player::isGamer).size();
    }

    /** Number of players still in the round: active gamers with at least one card. */
    static int countActiveWithCards(TurnOrder players) {
        return activeWithCards(players).size();
    }

    /**
     * Fallback loser for a capped (non-terminating) round: the active player with
     * the most cards in hand. On a tie the first by seating order wins, matching
     * the previous strict {@code >} scan (see {@code rules.md} §8 tie-break note in
     * the review). Returns {@code null} when no one is active with cards.
     */
    static Player mostCards(TurnOrder players) {
        return activeWithCards(players).stream()
                .max(Comparator.comparingInt((Player p) -> p.getHand().size()))
                .orElse(null);
    }

    /**
     * The first active player with cards in seating order — the loser on a
     * normally terminated round. Returns {@code null} when no one is active with
     * cards.
     */
    static Player firstWithCards(TurnOrder players) {
        return activeWithCards(players).stream().findFirst().orElse(null);
    }
}
