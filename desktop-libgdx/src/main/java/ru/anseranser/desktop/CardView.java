package ru.anseranser.desktop;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import ru.anseranser.i18n.CardLocalizer;
import ru.anseranser.model.Card;

/**
 * A single card rendered as a rounded rectangle with a short localized label
 * (e.g. "AS", "7H") produced by {@link CardLocalizer} in the {@code LETTERS}
 * style. No image asset is required — the suit letter and colour come from the
 * system font.
 *
 * <p>Used by {@link GameScreen} for hands, the bank and the scoreboard stacks.
 * When {@code onClick} is provided the card becomes clickable (used for the
 * human's hand to feed {@link DesktopInputProvider}).</p>
 */
final class CardView extends Container<Label> {

    private static final float CARD_W = 52f;
    private static final float CARD_H = 72f;

    interface ClickHandler {
        void accept(Card card);
    }

    CardView(Card card, Skin skin, CardLocalizer localizer) {
        this(card, skin, localizer, null);
    }

    CardView(Card card, Skin skin, CardLocalizer localizer, ClickHandler onClick) {
        Label label = new Label(localizer.cardName(card, CardLocalizer.Style.LETTERS), skin);
        label.setColor(suitColor(card.suit()));
        setActor(label);
        setSize(CARD_W, CARD_H);
        pad(4f);
        if (skin.has("card", com.badlogic.gdx.scenes.scene2d.utils.Drawable.class)) {
            setBackground(skin.getDrawable("card"));
        }
        if (onClick != null) {
            addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    onClick.accept(card);
                }
            });
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
