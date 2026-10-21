package ru.anseranser.engine;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
 * How a game is set up: the rank set, the trump suits and the seed of the
 * random generator.
 *
 * <p>Every suit in play belongs to exactly one seat, so the trump suits are the
 * suits of the table and must be distinct.
 */
public record Setup(RankSet rankSet, List<Card.Suit> trumpSuits, long seed) {

    public Setup {
        Objects.requireNonNull(rankSet, "rankSet");
        trumpSuits = List.copyOf(trumpSuits);
        if (trumpSuits.size() < 2 || trumpSuits.size() > 4) {
            throw new IllegalArgumentException(
                    "a game is played by 2 to 4 seats, got trump suits " + trumpSuits);
        }
        if (new HashSet<>(trumpSuits).size() != trumpSuits.size()) {
            throw new IllegalArgumentException("trump suits must be distinct, got " + trumpSuits);
        }
    }
}
