package ru.anseranser.desktop;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import org.junit.jupiter.api.Test;
import ru.anseranser.engine.Card;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CardViewTest {

    private static final Card SEVEN = new Card(Card.Suit.SPADES, Card.Rank.SEVEN);
    private static final Card ACE = new Card(Card.Suit.SPADES, Card.Rank.ACE);

    @Test
    void aLegalCardIsMarkedAndTheOthersAreNot() {
        CardView legal = view(SEVEN);
        CardView illegal = view(ACE);

        CardView.markLegal(List.of(legal, illegal), Set.of(SEVEN));

        assertEquals(Color.GREEN, legal.getActor().getColor());
        assertEquals(Color.WHITE, illegal.getActor().getColor());
    }

    @Test
    void withoutALegalCardEveryCardStaysUnmarked() {
        CardView view = view(SEVEN);

        CardView.markLegal(List.of(view), Set.of());

        assertEquals(Color.WHITE, view.getActor().getColor());
    }

    @Test
    void aMarkedCardIsUnmarkedAgain() {
        CardView view = view(SEVEN);

        view.markPlayable();
        view.clearMark();

        assertEquals(Color.WHITE, view.getActor().getColor());
    }

    private static CardView view(Card card) {
        return new CardView(new TextureRegion(), card, null);
    }
}
