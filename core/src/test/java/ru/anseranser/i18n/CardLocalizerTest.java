package ru.anseranser.i18n;

import org.junit.jupiter.api.Test;
import ru.anseranser.model.Card;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies {@link CardLocalizer}: English vs Russian names, full word form vs
 * compact glyph form, and that suit symbols are the expected Unicode glyphs.
 */
class CardLocalizerTest {

    private static final Card ACE_SPADES = new Card(Card.Suit.SPADES, Card.Rank.ACE);
    private static final Card SEVEN_HEARTS = new Card(Card.Suit.HEARTS, Card.Rank.SEVEN);
    private static final Card TEN_DIAMONDS = new Card(Card.Suit.DIAMONDS, Card.Rank.TEN);

    @Test
    void full_english() {
        CardLocalizer en = new CardLocalizer(new Messages(Locale.ENGLISH), CardLocalizer.Style.FULL);
        assertEquals("Ace of Spades", en.cardName(ACE_SPADES));
        assertEquals("Seven of Hearts", en.cardName(SEVEN_HEARTS));
        assertEquals("Ten of Diamonds", en.cardName(TEN_DIAMONDS));
    }

    @Test
    void full_russian() {
        CardLocalizer ru = new CardLocalizer(new Messages(new Locale("ru")), CardLocalizer.Style.FULL);
        assertEquals("Туз пик", ru.cardName(ACE_SPADES));
        assertEquals("Семёрка червей", ru.cardName(SEVEN_HEARTS));
        assertEquals("Десятка бубён", ru.cardName(TEN_DIAMONDS));
    }

    @Test
    void short_english() {
        CardLocalizer en = new CardLocalizer(new Messages(Locale.ENGLISH), CardLocalizer.Style.SHORT);
        assertEquals("A\u2660", en.cardName(ACE_SPADES));
        assertEquals("7\u2665", en.cardName(SEVEN_HEARTS));
        assertEquals("10\u2666", en.cardName(TEN_DIAMONDS));
    }

    @Test
    void short_russian() {
        CardLocalizer ru = new CardLocalizer(new Messages(new Locale("ru")), CardLocalizer.Style.SHORT);
        assertEquals("Т\u2660", ru.cardName(ACE_SPADES));
        assertEquals("7\u2665", ru.cardName(SEVEN_HEARTS));
        assertEquals("10\u2666", ru.cardName(TEN_DIAMONDS));
    }

    @Test
    void suitSymbolsAreUniform() {
        // Suit symbols are international — identical across locales.
        CardLocalizer en = new CardLocalizer(new Messages(Locale.ENGLISH), CardLocalizer.Style.SHORT);
        CardLocalizer ru = new CardLocalizer(new Messages(new Locale("ru")), CardLocalizer.Style.SHORT);
        for (Card.Suit s : Card.Suit.values()) {
            assertEquals(en.suitSymbol(s), ru.suitSymbol(s), "symbol for " + s + " must match across locales");
        }
        assertEquals("\u2660", en.suitSymbol(Card.Suit.SPADES));
        assertEquals("\u2663", en.suitSymbol(Card.Suit.CLUBS));
        assertEquals("\u2666", en.suitSymbol(Card.Suit.DIAMONDS));
        assertEquals("\u2665", en.suitSymbol(Card.Suit.HEARTS));
    }

    @Test
    void defaultLocalizerDoesNotThrowAndIsFullStyle() {
        // No-arg constructor uses the JVM default locale; we only assert it
        // produces a FULL (no suit symbols) form and does not blow up.
        CardLocalizer def = new CardLocalizer();
        String name = def.cardName(ACE_SPADES);
        assertFalse(name.contains("\u2660"), "default (FULL) localizer must not use suit symbols: " + name);
    }

    @Test
    void englishLocalizerIsFullWithoutSymbols() {
        CardLocalizer en = new CardLocalizer(new Messages(Locale.ENGLISH), CardLocalizer.Style.FULL);
        String name = en.cardName(ACE_SPADES);
        assertTrue(name.toLowerCase().contains("spades"), "explicit EN localizer: " + name);
        assertFalse(name.contains("\u2660"), "FULL style must not use suit symbols: " + name);
    }
}
