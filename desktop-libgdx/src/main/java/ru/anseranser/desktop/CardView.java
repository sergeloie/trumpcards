package ru.anseranser.desktop;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import ru.anseranser.i18n.CardLocalizer;
import ru.anseranser.model.Card;

/**
 * A single card rendered as a rounded rectangle with a short localized label
 * (e.g. "A\u2660", "7\u2665") produced by {@link CardLocalizer}. No image asset
 * is required — the suit glyph and colour come from the system font.
 *
 * <p>Used by {@link GameScreen} for hands, the bank and the scoreboard stacks.
 */
final class CardView extends Container<Label> {

    private static final float CARD_W = 52f;
    private static final float CARD_H = 72f;

    CardView(Card card, Skin skin, CardLocalizer localizer) {
        Label label = new Label(localizer.cardName(card, CardLocalizer.Style.LETTERS), skin);
        label.setColor(suitColor(card.suit()));
        setActor(label);
        setSize(CARD_W, CARD_H);
        // Rounded-rect look via padding + background if the skin provides one.
        pad(4f);
        if (skin.has("card", com.badlogic.gdx.scenes.scene2d.utils.Drawable.class)) {
            setBackground(skin.getDrawable("card"));
        }
    }

    private static Color suitColor(Card.Suit suit) {
        return switch (suit) {
            case HEARTS, DIAMONDS -> Color.RED;
            case SPADES, CLUBS -> Color.BLACK;
        };
    }

    float cardWidth() { return CARD_W; }
    float cardHeight() { return CARD_H; }
}
