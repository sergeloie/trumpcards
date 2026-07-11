package ru.anseranser.i18n;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessagesTest {

    @Test
    void englishDefaultUsesBundledPatternsWithArgs() {
        Messages en = new Messages(Locale.ENGLISH);
        assertEquals("=== THE GAME BEGINS ===", en.get("game.started"));
        assertEquals("Winner is: SPADES", en.get("game.winner", "SPADES"));
        // Single quote must survive doubling in the pattern ("can''t" -> "can't").
        assertTrue(en.get("event.pot_taken", "DIAMONDS", "ACE", 9)
                .contains("can't beat ACE -> take pot (9 cards)"));
    }

    @Test
    void russianBundleOverridesDefault() {
        Messages ru = new Messages(new Locale("ru"));
        assertEquals("=== ИГРА НАЧИНАЕТСЯ ===", ru.get("game.started"));
        assertEquals("Победитель: SPADES", ru.get("game.winner", "SPADES"));
        assertTrue(ru.get("event.pot_taken", "DIAMONDS", "ACE", 9)
                .contains("не может побить ACE -> забирает банк (9 карт)"));
    }

    @Test
    void missingKeyFallsBackToKey() {
        Messages en = new Messages(Locale.ENGLISH);
        assertEquals("no.such.key", en.get("no.such.key"));
    }
}
