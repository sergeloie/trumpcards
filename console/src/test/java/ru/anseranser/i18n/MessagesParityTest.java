package ru.anseranser.i18n;

import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MessagesParityTest {

    @Test
    void russianBundleHasEveryKeyOfTheDefaultBundle() {
        assertContainsAll(ResourceBundle.getBundle("messages", Locale.ROOT), "messages.properties");
    }

    @Test
    void russianBundleHasEveryKeyOfTheEnglishBundle() {
        assertContainsAll(ResourceBundle.getBundle("messages", Locale.ENGLISH), "messages_en.properties");
    }

    private void assertContainsAll(ResourceBundle english, String name) {
        ResourceBundle russian = ResourceBundle.getBundle("messages", Locale.of("ru"));
        Set<String> missing = new TreeSet<>();
        for (String key : english.keySet()) {
            if (!russian.containsKey(key)) {
                missing.add(key);
            }
        }
        assertEquals(Set.of(), missing, "the Russian bundle is missing the keys of " + name);
    }
}
