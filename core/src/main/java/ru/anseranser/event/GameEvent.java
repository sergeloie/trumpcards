package ru.anseranser.event;

import ru.anseranser.model.Card;
import ru.anseranser.model.Player;

import java.util.List;
import java.util.Map;

/**
 * Events emitted by the game engine. The core (model/engine) never formats or
 * prints text itself; it only produces these data-carrying events so that any
 * presentation layer (console, desktop UI, mobile UI) can render them.
 *
 * This is the seam introduced in refactor Stage 1: it decouples game rules
 * from I/O, which is a prerequisite for porting the game to other platforms.
 */
public sealed interface GameEvent permits
        GameEvent.GameStarted,
        GameEvent.RoundStarted,
        GameEvent.CardPlayed,
        GameEvent.CardBeaten,
        GameEvent.PotTaken,
        GameEvent.RoundEnded,
        GameEvent.GameEnded {

    /** Fired once when {@code playGame()} starts. */
    record GameStarted() implements GameEvent {}

    /** Fired at the beginning of each round with the full readable state. */
    record RoundStarted(
            Player dealer,
            Map<Card.Suit, List<Card>> scoreboard,
            Map<Player, List<Card>> hands
    ) implements GameEvent {}

    /** A player led (played the first card of a trick). */
    record CardPlayed(Player player, Card card) implements GameEvent {}

    /** A player beat the attacking card with another card. */
    record CardBeaten(Player player, Card attacking, Card beating) implements GameEvent {}

    /** A player could not beat the top card and took the whole pot. */
    record PotTaken(Player player, Card topCard, int potSize) implements GameEvent {}

    /** A round finished; {@code pushedToScoreboard} is null when nothing was pushed. */
    record RoundEnded(
            Player loser,
            Card pushedToScoreboard,
            Map<Card.Suit, List<Card>> scoreboard,
            boolean eliminated
    ) implements GameEvent {}

    /** Fired once when the game ends, with the surviving winner. */
    record GameEnded(Player winner) implements GameEvent {}
}
