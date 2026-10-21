package ru.anseranser.bots;

import ru.anseranser.engine.Card;
import ru.anseranser.engine.Move;
import ru.anseranser.engine.State;

import java.util.Comparator;
import java.util.List;

/**
 * The rule bot: the fixed policy of the old engine's decision strategy.
 *
 * <p>A lead takes the first card of the hand that minimises (is-trump, rank),
 * a defence takes the first of the legal moves, which the engine already lists
 * sorted weakest first.
 */
public final class RuleBot {

    private RuleBot() {
    }

    /**
     * The move the bot takes: the index in the list of legal moves.
     *
     * @throws IllegalArgumentException when the game is over and there is no move to choose
     */
    public static int choose(State state, List<Move> moves) {
        if (moves.isEmpty()) {
            throw new IllegalArgumentException("the game is over, there is no move to choose");
        }
        if (!state.leadRequired() && !state.pot().isEmpty()) {
            return 0;
        }
        return lead(state, moves);
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

    /** The card a move plays. */
    private static Card card(Move move) {
        return ((Move.Play) move).card();
    }
}
