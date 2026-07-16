package ru.anseranser.event;

import ru.anseranser.model.Card;

import java.util.List;
import java.util.Map;

/**
 * Events emitted by the game engine. The core (model/engine) never formats or
 * prints text itself; it only produces these data-carrying events so that any
 * presentation layer (console, desktop UI, mobile UI, web UI) can render them.
 *
 * <p>Players are identified by their fixed trump suit ({@link Card.Suit})
 * rather than by a {@code Player} reference. This keeps events immutable and
 * presentation-only: a listener can never accidentally mutate a domain object,
 * and the same event stream works for any UI target.</p>
 *
 * <p>This is the seam introduced in refactor Stage 1: it decouples game rules
 * from I/O, which is a prerequisite for porting the game to other platforms.</p>
 */
public sealed interface GameEvent permits
        GameEvent.GameStarted,
        GameEvent.AwaitingHumanInput,
        GameEvent.RoundStarted,
        GameEvent.CardPlayed,
        GameEvent.CardBeaten,
        GameEvent.PotTaken,
        GameEvent.RoundEnded,
        GameEvent.GameEnded {

    /** Fired once when {@code playGame()} starts. */
    record GameStarted() implements GameEvent {}

    /** Fired right before the engine blocks waiting for a human choice. */
    record AwaitingHumanInput(Card.Suit player) implements GameEvent {}

    /** Fired at the beginning of each round with the full readable state. */
    record RoundStarted(
            Card.Suit dealer,
            Map<Card.Suit, List<Card>> scoreboard,
            Map<Card.Suit, List<Card>> hands
    ) implements GameEvent {}

    /** A player led (played the first card of a trick). */
    record CardPlayed(Card.Suit player, Card card) implements GameEvent {}

    /** A player beat the attacking card with another card. */
    record CardBeaten(Card.Suit player, Card attacking, Card beating) implements GameEvent {}

    /** A player could not beat the top card and took the whole pot. */
    record PotTaken(Card.Suit player, Card topCard, int potSize) implements GameEvent {}

    /** A round finished; {@code pushedToScoreboard} is null when nothing was pushed. */
    record RoundEnded(
            Card.Suit loser,
            Card pushedToScoreboard,
            Map<Card.Suit, List<Card>> scoreboard,
            boolean eliminated
    ) implements GameEvent {}

    /** Fired once when the game ends, with the surviving winner. */
    record GameEnded(Card.Suit winner) implements GameEvent {}
}
