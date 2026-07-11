package ru.anseranser.i18n;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Thin wrapper over {@link ResourceBundle} for localized, parameterized strings.
 *
 * Introduced in refactor Stage 6 to externalize every user-facing string that
 * used to be hardcoded in the console presentation layer
 * ({@code ConsoleGameListener}, {@code ConsoleInputProvider}). Centralizing
 * strings here is the prerequisite for porting the game to other languages and
 * platforms without touching the rendering code.
 *
 * <p>Bundle files live on the classpath as {@code messages.properties}
 * (default / English) and {@code messages_<locale>.properties} (e.g.
 * {@code messages_ru.properties}). Patterns use {@link MessageFormat} syntax
 * ({@code {0}}, {@code {1}}, ...). A single quote in a pattern must be doubled
 * ({@code ''}) so {@link MessageFormat} does not treat it as an escape.</p>
 */
public class Messages {

    private final ResourceBundle bundle;

    /** Uses the JVM default locale. */
    public Messages() {
        this(Locale.getDefault());
    }

    /** Uses an explicit locale (e.g. {@code new Locale("ru")}). */
    public Messages(Locale locale) {
        this.bundle = ResourceBundle.getBundle("messages", locale);
    }

    /**
     * Resolve {@code key}, formatting it with {@code args} via MessageFormat.
     * If the key is missing from the bundle, the key itself is returned so the
     * UI never blows up on a forgotten translation.
     */
    public String get(String key, Object... args) {
        String pattern;
        try {
            pattern = bundle.getString(key);
        } catch (MissingResourceException e) {
            return key;
        }
        if (args == null || args.length == 0) {
            return pattern;
        }
        return MessageFormat.format(pattern, args);
    }
}
