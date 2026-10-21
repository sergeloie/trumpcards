package ru.anseranser.engine;

import java.util.Comparator;
import java.util.List;

/**
 * The policy of the old engine's {@code AiDecisionStrategy}: a lead takes the
 * first card of the hand that minimises (is-trump, rank), a defence takes the
 * first of the legal moves, which the engine already lists sorted.
 *
 * <p>The rule bot of the {@code bots} module replaces this helper; until then
 * the golden gate runs on it.
 */
final class RulePolicy {

    private RulePolicy() {
    }

    /** The move the policy takes: the index in the list of legal moves. */
    static int choose(State state, List<Move> moves) {
        if (moves.isEmpty()) {
            throw new IllegalArgumentException("the game is over, there is no move to choose");
        }
        if (state.leadRequired() || state.pot().isEmpty()) {
            return lead(state, moves);
        }
        return 0;
    }

    /**
     * The lead: the first card of the hand that minimises (is-trump, rank).
     * The first of the equal minima wins, as the minimum of a stream does.
     */
    private static int lead(State state, List<Move> moves) {
        Card.Suit trump = state.turn().trump();
        Comparator<Card> order = Comparator.comparing((Card card) -> card.suit() == trump)
                .thenComparing(card -> card.rank().ordinal());
        int best = 0;
        for (int i = 1; i < moves.size(); i++) {
            if (order.compare(card(moves.get(i)), card(moves.get(best))) < 0) {
                best = i;
            }
        }
        return best;
    }

    /**
     * The move the policy takes when a scenario pins the card that opens the
     * trick: the pinned card when the seat to move holds it and leads, and the
     * rule policy's move in every other case.
     */
    static int chooseWithPinnedLead(State state, List<Move> moves, Card pinned) {
        if (state.leadRequired() || state.pot().isEmpty()) {
            for (int i = 0; i < moves.size(); i++) {
                if (card(moves.get(i)).equals(pinned)) {
                    return i;
                }
            }
        }
        return choose(state, moves);
    }

    /** The card a move plays. */
    static Card card(Move move) {
        return ((Move.Play) move).card();
    }
}
