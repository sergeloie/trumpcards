package ru.anseranser.engine;

import java.util.List;

/**
 * The ladder of trump cards on a board: the rank it requires next.
 *
 * <p>The ladder is public because the observation of a learned policy needs
 * the rank a board requires next, and the rules keep the rest of the board to
 * themselves.
 */
public final class Ladders {

    private Ladders() {
    }

    /**
     * The rank the board requires next: the one after the top of the board.
     * It is absent when the ladder is complete.
     */
    public static Card.Rank nextRequiredRank(List<Card> board) {
        if (board.isEmpty()) {
            return null;
        }
        Card.Rank[] ranks = Card.Rank.values();
        int next = board.get(board.size() - 1).rank().ordinal() + 1;
        return next >= ranks.length ? null : ranks[next];
    }
}
