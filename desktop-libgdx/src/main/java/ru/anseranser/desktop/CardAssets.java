package ru.anseranser.desktop;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import ru.anseranser.model.Card;

import java.util.EnumMap;
import java.util.Map;

/**
 * Loads and caches the graphical card assets shipped under
 * {@code cards/base1/...} (copied from the project {@code asset/} catalog into
 * the desktop module's resources).
 *
 * <p>Card faces follow the naming convention
 * {@code <SuitWord>/<rank>-<SuitWord>.png} (e.g. {@code Clubs/7-Club.png}),
 * card backs live under {@code Card Backs/CardBack-<Color>.png}, and an
 * {@code EmptyCard.png} is used as a placeholder for an empty slot.</p>
 *
 * <p>Textures are loaded lazily and kept for the lifetime of the application;
 * call {@link #dispose()} on shutdown to free GPU memory.</p>
 */
final class CardAssets {

    private static final String BASE = "cards/base1/";

    /** Suit folder / file word, matching the asset catalog. */
    private static final Map<Card.Suit, String> SUIT_WORD = Map.of(
            Card.Suit.SPADES, "Spade",
            Card.Suit.CLUBS, "Club",
            Card.Suit.DIAMONDS, "Diamond",
            Card.Suit.HEARTS, "Heart");

    private final Map<Card, TextureRegion> faces = new java.util.HashMap<>();
    private final Map<Card.Suit, TextureRegion> backs = new EnumMap<>(Card.Suit.class);
    private TextureRegion empty;

    /** Load the back texture for a given seat colour (the seat's trump suit). */
    TextureRegion back(Card.Suit seat) {
        return backs.computeIfAbsent(seat, s -> load("Card Backs/CardBack-" + backColor(s) + ".png"));
    }

    /** Load the face texture for a concrete card. */
    TextureRegion face(Card card) {
        return faces.computeIfAbsent(card, c -> {
            String word = SUIT_WORD.get(c.suit());
            String file = rankCode(c.rank()) + "-" + word + ".png";
            return load(word + "s/" + file);
        });
    }

    /** Filename rank code, matching the asset catalog (2..10 digits, J/Q/K/A letters). */
    private static String rankCode(Card.Rank rank) {
        return switch (rank) {
            case TWO -> "2";
            case THREE -> "3";
            case FOUR -> "4";
            case FIVE -> "5";
            case SIX -> "6";
            case SEVEN -> "7";
            case EIGHT -> "8";
            case NINE -> "9";
            case TEN -> "10";
            case JACK -> "J";
            case QUEEN -> "Q";
            case KING -> "K";
            case ACE -> "A";
        };
    }

    /** Placeholder texture for an empty card slot. */
    TextureRegion empty() {
        if (empty == null) {
            empty = load("EmptyCard.png");
        }
        return empty;
    }

    private static String backColor(Card.Suit seat) {
        return switch (seat) {
            case SPADES -> "Blue";
            case CLUBS -> "Green";
            case DIAMONDS -> "Red";
            case HEARTS -> "Red";
        };
    }

    private TextureRegion load(String path) {
        Texture tex = new Texture(Gdx.files.internal(BASE + path));
        tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        return new TextureRegion(tex);
    }

    void dispose() {
        faces.values().forEach(r -> r.getTexture().dispose());
        backs.values().forEach(r -> r.getTexture().dispose());
        if (empty != null) {
            empty.getTexture().dispose();
        }
    }
}
