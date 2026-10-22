package ru.anseranser.i18n;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * The localized strings of a front end. The patterns live in the
 * {@code messages} resource bundle: the default one and the one of every
 * locale that is translated.
 *
 * <p>A pattern uses the {@link MessageFormat} syntax, and a literal single
 * quote is written twice ({@code ''}).</p>
 */
public class Messages {

    private final ResourceBundle bundle;

    /** Uses the JVM default locale. */
    public Messages() {
        this(Locale.getDefault());
    }

    /** Uses an explicit locale. */
    public Messages(Locale locale) {
        this.bundle = ResourceBundle.getBundle("messages", locale);
    }

    /**
     * The text of {@code key}, formatted with {@code args}. A key that the
     * bundle does not hold is answered with the key itself, so a forgotten
     * translation never breaks the screen.
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
