package ru.anseranser.i18n;

import ru.anseranser.model.Card;

import java.util.Locale;

/**
 * Localizes {@link Card}s for the presentation layer. The model ({@code Card})
 * intentionally stays locale-agnostic — its {@code toString()} keeps the neutral
 * "ACE of SPADES" form for logs — and every human-readable card name is produced
 * here, from the same {@code messages_*} ResourceBundle that {@link Messages} uses.
 *
 * <p>Two rendering styles are supported so one implementation serves both the
 * console and a future GUI / mobile target:
 * <ul>
 *   <li>{@link Style#FULL} — word form, e.g. English "ace of spades", Russian
 *       "туз пик". Safe for the Windows console (no Unicode suit glyphs).</li>
 *   <li>{@link Style#SHORT} — compact glyph form, e.g. "A\u2660", "7\u2665"
 *       (rank short-glyph + suit symbol). Intended for card widgets in a GUI.</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>{@code
 *   CardLocalizer cards = new CardLocalizer(new Messages(new Locale("ru")), Style.FULL);
 *   System.out.println(cards.cardName(card)); // "туз пик"
 * }</pre>
 */
public class CardLocalizer {

    /** How a card name is rendered. */
    public enum Style {
        /** Word form: "ace of spades" / "туз пик". */
        FULL,
        /** Compact glyph form: "A\u2660" / "7\u2665". */
        SHORT
    }

    private final Messages messages;
    private final Style style;

    /** Full English words, default JVM locale, FULL style. */
    public CardLocalizer() {
        this(new Messages(), Style.FULL);
    }

    public CardLocalizer(Messages messages) {
        this(messages, Style.FULL);
    }

    public CardLocalizer(Messages messages, Style style) {
        this.messages = messages;
        this.style = style;
    }

    public CardLocalizer(Locale locale, Style style) {
        this(new Messages(locale), style);
    }

    /** Localized name of a suit (e.g. "Spades" / "Пики"). */
    public String suitName(Card.Suit suit) {
        return messages.get("card.suit." + suit.name().toLowerCase());
    }

    /** Suit symbol (e.g. "\u2660"). International, not language-specific. */
    public String suitSymbol(Card.Suit suit) {
        return messages.get("card.suit." + suit.name().toLowerCase() + ".symbol");
    }

    /** Localized name of a rank (e.g. "Ace" / "Туз"). */
    public String rankName(Card.Rank rank) {
        return messages.get("card.rank." + rank.name().toLowerCase());
    }

    /** Short rank glyph (e.g. "A" / "Т"). */
    public String rankShort(Card.Rank rank) {
        return messages.get("card.rank." + rank.name().toLowerCase() + ".short");
    }

    /**
     * Localized name of a card in this localizer's {@link Style}. With {@code FULL}
     * the result is the word form; with {@code SHORT} the compact glyph form.
     */
    public String cardName(Card card) {
        return cardName(card, style);
    }

    /** Localized name of a card in the requested style (overrides this instance's). */
    public String cardName(Card card, Style override) {
        if (override == Style.SHORT) {
            return rankShort(card.rank()) + suitSymbol(card.suit());
        }
        return messages.get("card.format.full", rankName(card.rank()), suitName(card.suit()));
    }
}
