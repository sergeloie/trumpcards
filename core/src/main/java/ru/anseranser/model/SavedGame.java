package ru.anseranser.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Portable, JSON-serializable snapshot of a game in progress (R10). It captures
 * everything needed to resume a game later on another device / platform
 * (mobile, web, desktop): the players and their hands, the scoreboard ladders,
 * the pot, the dealer's remaining deck pool, the RNG state, the round counters,
 * and the {@code GameDriver} flow state (whose turn, phase, async-input flags).
 *
 * <p>The domain engine never depends on JSON — this class is the only place that
 * touches Gson. A platform module persists/loads the JSON string (e.g. to a file
 * or {@code SharedPreferences}/{@code localStorage}); the engine only knows
 * {@link GameDriver#save()} and {@link Game#restore(SavedGame, java.util.function.Function)}.
 */
public record SavedGame(
        int version,
        List<PlayerState> players,
        String dealerSeatTrump,
        List<Card> pot,
        Map<Card.Suit, List<Card>> scoreboard,
        List<Card> deckPool,
        long rngSeed,
        int roundsPlayed,
        int cappedRounds,
        DriverState driver) {

    /** One player's persisted state. {@code human} lets the loader re-wire the right {@link DecisionStrategy}. */
    public record PlayerState(String trump, List<Card> hand, boolean gamer, boolean human) {}

    /** The {@code GameDriver} flow state, so a resumed game continues exactly where it paused. */
    public record DriverState(
            String phase,
            String currentTrump,
            int moves,
            boolean asyncInput,
            boolean awaitingHuman,
            boolean leadingNewTrick,
            String pendingAction,
            Card forcedCard) {}

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Card.class, new CardAdapter())
            .create();

    public String toJson() {
        return GSON.toJson(this);
    }

    public static SavedGame fromJson(String json) {
        return GSON.fromJson(json, SavedGame.class);
    }

    /** Compact, human-readable card encoding: {@code "SPADES_ACE"}. */
    private static final class CardAdapter extends TypeAdapter<Card> {
        @Override
        public void write(JsonWriter out, Card card) throws IOException {
            if (card == null) {
                out.nullValue();
                return;
            }
            out.value(card.suit() + "_" + card.rank());
        }

        @Override
        public Card read(JsonReader in) throws IOException {
            if (in.peek() == com.google.gson.stream.JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            String s = in.nextString();
            int sep = s.indexOf('_');
            Card.Suit suit = Card.Suit.valueOf(s.substring(0, sep));
            Card.Rank rank = Card.Rank.valueOf(s.substring(sep + 1));
            return new Card(suit, rank);
        }
    }
}
