package ru.anseranser.desktop;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup;
import ru.anseranser.i18n.CardLocalizer;
import ru.anseranser.model.Card;

import java.util.Comparator;
import java.util.List;

/**
 * Renders the game state with the layout requested for the desktop port:
 *
 * <pre>
 *  +----------------+-----------------------------+
 *  | scoreboard     |  (opponents hands, top)     |
 *  | (top-left)     |                             |
 *  |                +-----------------------------+
 *  |                |  POT (center of screen)     |
 *  +----------------+-----------------------------+
 *  | move-log (right, full height)  | human hand  |
 *  |                                | (bottom, centered) |
 *  +--------------------------------+-------------+
 * </pre>
 *
 * The screen is stateless w.r.t. the engine: {@link #render(GameSnapshot)}
 * rebuilds the widgets from an immutable {@link GameSnapshot} (captured on the
 * engine thread) every time an event fires. All widget mutation happens on the
 * LibGDX render thread.
 */
final class GameScreen extends Table {

    private final Skin skin;
    private final CardLocalizer localizer;
    private final DesktopInputProvider input;

    private final Label statusLabel;
    private final Table potTable;
    private final Table humanHandTable;
    private final VerticalGroup scoreboardGroup;
    private final VerticalGroup opponentsGroup;
    private final Table logTable;
    private ScrollPane logPane;

    private static final float LOG_PANE_WIDTH = 280f;
    private static final float LOG_LINE_WIDTH = 264f;

    GameScreen(Skin skin, CardLocalizer localizer, DesktopInputProvider input) {
        super(skin);
        this.skin = skin;
        this.localizer = localizer;
        this.input = input;

        statusLabel = new Label("", skin);
        potTable = new Table(skin);
        humanHandTable = new Table(skin);
        scoreboardGroup = new VerticalGroup();
        opponentsGroup = new VerticalGroup();
        logTable = new Table(skin);
        logTable.left().top();

        buildLayout();
    }

    private void buildLayout() {
        setFillParent(true);
        pad(8f);

        // Top row: scoreboard (left) + opponents (right)
        Table topRow = new Table(skin);
        topRow.left().top();
        topRow.add(scoreboardGroup).left().top().padRight(24f);
        topRow.add(opponentsGroup).expandX().left().top();

        // Center: pot
        Table centerRow = new Table(skin);
        centerRow.add(new Label("POT", skin)).row();
        centerRow.add(potTable);

        // Bottom: human hand (left/center) + log (right)
        logPane = new ScrollPane(logTable, skin);
        logPane.setFadeScrollBars(false);
        logPane.setScrollingDisabled(true, false);

        Table bottomRow = new Table(skin);
        bottomRow.add(new Label("YOUR HAND", skin)).colspan(2).center().row();
        bottomRow.add(humanHandTable).expandX().center();
        bottomRow.add(logPane).width(LOG_PANE_WIDTH).top().right();

        add(statusLabel).colspan(2).left().row();
        add(topRow).colspan(2).expandX().fillX().top().row();
        add(centerRow).colspan(2).expand().top().row();
        add(bottomRow).colspan(2).expandX().fillX().bottom();
    }

    /**
     * Rebuild all widgets from an immutable {@link GameSnapshot} captured on the
     * engine thread. Must be invoked from the LibGDX render thread only (the
     * listener posts it via {@code Gdx.app.postRunnable}).
     */
    void render(GameSnapshot snap) {
        statusLabel.setText(CardView.ascii(snap.status()));

        // Scoreboard (left, top): show only the most recent card placed on each
        // suit's ladder. The card name already encodes the suit (e.g. "8H"), so a
        // full stack ("8H 7H 6H") is unnecessary clutter.
        scoreboardGroup.clear();
        snap.scoreboard().forEach((suit, stack) -> {
            String label = stack.isEmpty()
                    ? "-"
                    : localizer.cardName(stack.get(stack.size() - 1), CardLocalizer.Style.LETTERS);
            scoreboardGroup.addActor(new Label(CardView.ascii(label), skin));
        });

        // Opponents (top-right): every gamer except the human seat (SPADES)
        opponentsGroup.clear();
        for (GameSnapshot.OpponentView o : snap.opponents()) {
            Label l = new Label(CardView.ascii(o.trump() + "  (" + o.cardCount() + " cards)"), skin);
            opponentsGroup.addActor(l);
        }

        // Pot (center)
        potTable.clear();
        for (Card c : snap.pot()) {
            potTable.add(new CardView(c, skin, localizer)).pad(2f);
        }

        // Human hand (bottom) — laid out as 4 rows, one per suit, in Card.Suit
        // order (SPADES on top, then CLUBS, DIAMONDS, HEARTS). Within a suit the
        // cards are sorted by ascending rank. Cards are clickable while awaiting input.
        humanHandTable.clear();
        List<Card> valid = input.validChoices();
        boolean awaiting = !valid.isEmpty();
        for (Card.Suit suit : Card.Suit.values()) {
            List<Card> group = snap.humanHand().stream()
                    .filter(c -> c.suit() == suit)
                    .sorted(Comparator.comparingInt(c -> c.rank().getValue()))
                    .toList();
            Table suitRow = new Table(skin);
            for (Card c : group) {
                CardView view = new CardView(c, skin, localizer,
                        card -> input.onCardClicked(card));
                if (awaiting && valid.contains(c)) {
                    view.getActor().setColor(Color.GREEN);
                }
                suitRow.add(view).pad(2f);
            }
            humanHandTable.add(suitRow).center().row();
        }

        // Move log (right panel) — each entry on its own horizontal line, wrapped
        // to a fixed width so text flows left-to-right (not one letter per line).
        logTable.clear();
        for (String line : snap.logLines()) {
            Label l = new Label(CardView.ascii(line), skin);
            l.setWrap(true);
            logTable.add(l).width(LOG_LINE_WIDTH).left().row();
        }
        // Auto-scroll to the newest entry at the bottom.
        logPane.validate();
        logPane.setScrollPercentY(1f);
    }
}
