package ru.anseranser.desktop;

import org.junit.jupiter.api.Test;
import ru.anseranser.i18n.CardLocalizer;
import ru.anseranser.model.Card;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guard against the GlyphLayout NPE: the default LibGDX BitmapFont only ships
 * ASCII glyphs, so any non-ASCII text reaching a Label throws
 * "Cannot read field fixedWidth because glyph is null". Every string the
 * desktop UI sends to a Label must stay within ASCII.
 */
class AsciiSafeTextTest {

    private static boolean isAscii(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) > 127) return false;
        }
        return true;
    }

    @Test
    void letterStyleCardNames_areAscii() {
        CardLocalizer loc = new CardLocalizer(CardLocalizer.Style.LETTERS);
        for (Card.Suit suit : Card.Suit.values()) {
            for (Card.Rank rank : Card.Rank.values()) {
                String name = loc.cardName(new Card(suit, rank), CardLocalizer.Style.LETTERS);
                assertTrue(isAscii(name), "non-ASCII card name: " + name);
            }
        }
    }

    @Test
    void suitLetters_areAscii() {
        CardLocalizer loc = new CardLocalizer(CardLocalizer.Style.LETTERS);
        for (Card.Suit suit : Card.Suit.values()) {
            assertTrue(isAscii(loc.suitLetter(suit)), "non-ASCII suit letter: " + loc.suitLetter(suit));
        }
    }

    @Test
    void asciiHelper_replacesNonAscii() {
        assertEquals("SPADES", CardView.ascii("SPADES"));
        assertEquals("????", CardView.ascii("Пики"));
        assertEquals("", CardView.ascii(null));
    }
}
