package ru.anseranser.desktop;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import ru.anseranser.model.Card;

/**
 * A single card rendered as a graphical asset (a {@link TextureRegion} from
 * {@link CardAssets}) instead of a text label. Used by {@link GameScreen} for
 * the human hand, the pot, the scoreboard stacks and the opponent card-backs.
 *
 * <p>When {@code onClick} is provided the card becomes clickable (used for the
 * human's hand to feed {@link DesktopInputProvider}). A green tint marks a card
 * that is currently a valid choice.</p>
 */
final class CardView extends Container<Image> {

    static final float CARD_W = 96f;
    static final float CARD_H = 132f;

    interface ClickHandler {
        void accept(Card card);
    }

    CardView(TextureRegion region, Card card, ClickHandler onClick) {
        Image image = new Image(region);
        setActor(image);
        setSize(CARD_W, CARD_H);
        image.setFillParent(true);
        if (onClick != null && card != null) {
            addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    onClick.accept(card);
                }
            });
        }
    }

    /** A face-up card (clickable when {@code onClick} is non-null). */
    static CardView face(Card card, CardAssets assets, ClickHandler onClick) {
        return new CardView(assets.face(card), card, onClick);
    }

    /** A face-down card back for the given seat. */
    static CardView back(Card.Suit seat, CardAssets assets) {
        return new CardView(assets.back(seat), null, null);
    }

    /** An empty placeholder slot. */
    static CardView empty(CardAssets assets) {
        return new CardView(assets.empty(), null, null);
    }

    /** Tint the card green to mark it as a currently valid choice. */
    void markPlayable() {
        getActor().setColor(Color.GREEN);
    }

    void clearMark() {
        getActor().setColor(Color.WHITE);
    }

    /** Rotate the whole card widget by the given degrees (e.g. 90 for side seats). */
    void setCardRotation(float degrees) {
        setRotation(degrees);
        // Keep the widget's footprint square so layout math stays simple.
        setSize(CARD_H, CARD_W);
    }

    /**
     * Return a copy of {@code s} with every non-ASCII character replaced by '?'.
     * The default LibGDX {@code BitmapFont} only ships ASCII glyphs, so any
     * non-ASCII text reaching a {@code Label} throws a GlyphLayout NPE. A null
     * input yields an empty string.
     */
    static String ascii(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            sb.append(c > 127 ? '?' : c);
        }
        return sb.toString();
    }
}
