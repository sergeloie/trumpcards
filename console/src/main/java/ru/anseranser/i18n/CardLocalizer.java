package ru.anseranser.i18n;

import ru.anseranser.engine.Card;

import java.util.Locale;

/**
 * The localized names of the cards and the suits. The card itself stays
 * locale-agnostic; every name a person reads is produced here, from the same
 * bundle {@link Messages} uses.
 *
 * <p>Three styles are offered: the word form, the compact glyph form and the
 * compact letter form. The word form is safe for a terminal, the letter form
 * is safe for a font that has no glyphs.</p>
 */
public class CardLocalizer {

    /** How a card name is rendered. */
    public enum Style {
        /** The word form: "ace of spades". */
        FULL,
        /** The compact glyph form: "A♠". */
        SHORT,
        /** The compact letter form: "AS". */
        LETTERS
    }

    private final Messages messages;
    private final Style style;

    /** The word form, in the JVM default locale. */
    public CardLocalizer() {
        this(new Messages(), Style.FULL);
    }

    /** The given style, in the JVM default locale. */
    public CardLocalizer(Style style) {
        this(new Messages(), style);
    }

    /** The word form, in the locale of {@code messages}. */
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

    /** The name of a suit. */
    public String suitName(Card.Suit suit) {
        return messages.get("card.suit." + suit.name().toLowerCase());
    }

    /** The symbol of a suit, the same in every language. */
    public String suitSymbol(Card.Suit suit) {
        return messages.get("card.suit." + suit.name().toLowerCase() + ".symbol");
    }

    /** The name of a rank. */
    public String rankName(Card.Rank rank) {
        return messages.get("card.rank." + rank.name().toLowerCase());
    }

    /** The short glyph of a rank. */
    public String rankShort(Card.Rank rank) {
        return messages.get("card.rank." + rank.name().toLowerCase() + ".short");
    }

    /** The rank code of the letter style: 6, 7, 8, 9, 10, J, Q, K, A. */
    public String rankLetter(Card.Rank rank) {
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

    /** The name of a card in the style of this localizer. */
    public String cardName(Card card) {
        return cardName(card, style);
    }

    /** The name of a card in the given style. */
    public String cardName(Card card, Style override) {
        if (override == Style.SHORT) {
            return rankShort(card.rank()) + suitSymbol(card.suit());
        }
        if (override == Style.LETTERS) {
            return rankLetter(card.rank()) + suitLetter(card.suit());
        }
        return messages.get("card.format.full", rankName(card.rank()), suitName(card.suit()));
    }

    /** The suit code of the letter style: S, H, D, C. */
    public String suitLetter(Card.Suit suit) {
        return switch (suit) {
            case SPADES -> "S";
            case HEARTS -> "H";
            case DIAMONDS -> "D";
            case CLUBS -> "C";
        };
    }
}
