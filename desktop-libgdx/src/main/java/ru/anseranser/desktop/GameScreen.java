package ru.anseranser.desktop;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup;
import ru.anseranser.i18n.CardLocalizer;
import ru.anseranser.model.Card;

import java.util.Comparator;
import java.util.List;

/**
 * Renders the game state with a fixed 1920x1080 table layout:
 *
 * <pre>
 *  +--------------------------------------------------+
 *  |  scoreboard (top-left)   opponent TOP (top)      |
 *  |                                                |
 *  |  opponent LEFT (left)    POT (center)   opponent RIGHT (right) |
 *  |                                                |
 *  |              YOUR HAND (bottom, centered)         |
 *  +--------------------------------------------------+
 * </pre>
 *
 * The human (SPADES) sits at the bottom. Opponents are drawn as a single
 * face-down card back (rubashka) with the number of cards in their hand and
 * their trump suit, placed LEFT / TOP / RIGHT according to turn order. The pot
 * sits in the middle. All cards and backs are shown vertically; the left and
 * right opponents' cards are rotated 90° so their long edge faces the centre.
 *
 * <p>The screen is stateless w.r.t. the engine: {@link #render(GameSnapshot)}
 * rebuilds the widgets from an immutable {@link GameSnapshot} (captured on the
 * engine thread) every time an event fires. All widget mutation happens on the
 * LibGDX render thread.</p>
 */
final class GameScreen extends Table {

    private final Skin skin;
    private final CardLocalizer localizer;
    private final DesktopInputProvider input;
    private final CardAssets assets;

    private final Label statusLabel;
    private final Table potTable;
    private final Table humanHandTable;
    private final VerticalGroup scoreboardGroup;
    private final Table leftOpponent;
    private final Table topOpponent;
    private final Table rightOpponent;
    private final Table humanPanel;

    GameScreen(Skin skin, CardLocalizer localizer, DesktopInputProvider input, CardAssets assets) {
        super(skin);
        this.skin = skin;
        this.localizer = localizer;
        this.input = input;
        this.assets = assets;

        statusLabel = new Label("", skin);
        potTable = new Table(skin);
        humanHandTable = new Table(skin);
        scoreboardGroup = new VerticalGroup();
        leftOpponent = new Table(skin);
        topOpponent = new Table(skin);
        rightOpponent = new Table(skin);
        humanPanel = new Table(skin);

        buildLayout();
    }

    private void buildLayout() {
        setSize(1920f, 1080f);
        setFillParent(true);
        pad(16f);

        // Scoreboard (top-left)
        Table scoreboardPanel = new Table(skin);
        scoreboardPanel.add(new Label("SCOREBOARD", skin)).left().row();
        scoreboardPanel.add(scoreboardGroup).left().top();

        // Center: pot
        Table centerPanel = new Table(skin);
        centerPanel.add(new Label("POT", skin)).row();
        centerPanel.add(potTable);

        // Human hand (bottom)
        humanPanel.add(new Label("YOUR HAND", skin)).center().row();
        humanPanel.add(humanHandTable).expandX().center();

        // Row 1: scoreboard (left) | top opponent (center)
        add(scoreboardPanel).top().left();
        add(topOpponent).top().expandX().center().row();

        // Row 2: left opponent | pot (center) | right opponent
        add(leftOpponent).top().left();
        add(centerPanel).expand().center();
        add(rightOpponent).top().right().row();

        // Row 3: human hand (bottom, centered)
        add(new Table(skin)).left();
        add(humanPanel).bottom().expandX().center();
        add(new Table(skin)).right().row();

        // Status line pinned to the very bottom-left
        add(statusLabel).colspan(3).left().bottom();
    }

    /**
     * Rebuild all widgets from an immutable {@link GameSnapshot} captured on the
     * engine thread. Must be invoked from the LibGDX render thread only.
     */
    void render(GameSnapshot snap) {
        statusLabel.setText(snap.status());

        // Scoreboard (top-left): most recent card on each suit's ladder.
        scoreboardGroup.clear();
        snap.scoreboard().forEach((suit, stack) -> {
            String label = stack.isEmpty()
                    ? "-"
                    : localizer.cardName(stack.get(0), CardLocalizer.Style.LETTERS);
            scoreboardGroup.addActor(new Label(label, skin));
        });

        // Opponents: one face-down card back (rotated for side seats) + card count
        // + trump suit, placed by seat.
        leftOpponent.clear();
        topOpponent.clear();
        rightOpponent.clear();
        for (GameSnapshot.OpponentView o : snap.opponents()) {
            Table panel = switch (o.seat()) {
                case LEFT -> leftOpponent;
                case TOP -> topOpponent;
                case RIGHT -> rightOpponent;
            };
            CardView back = CardView.back(o.trump(), assets);
            if (o.seat() == GameSnapshot.Seat.LEFT) {
                back.setCardRotation(90f);
            } else if (o.seat() == GameSnapshot.Seat.RIGHT) {
                back.setCardRotation(-90f);
            }
            panel.add(back).pad(4f).row();
            panel.add(new Label(o.cardCount() + " cards", skin)).center().row();
            panel.add(new Label("Trump: " + o.trump().name(), skin)).center();
        }

        // Pot (center)
        potTable.clear();
        if (snap.pot().isEmpty()) {
            potTable.add(CardView.empty(assets)).pad(2f);
        } else {
            for (Card c : snap.pot()) {
                potTable.add(CardView.face(c, assets, null)).pad(2f);
            }
        }

        // Human hand (bottom) — 4 rows, one per suit, ascending rank.
        humanHandTable.clear();
        List<Card> valid = input.validChoices();
        boolean awaiting = !valid.isEmpty();
        for (Card.Suit suit : Card.Suit.values()) {
            List<Card> group = snap.humanHand().stream()
                    .filter(c -> c.suit() == suit)
                    .sorted(Comparator.comparingInt(c -> c.rank().getValue()))
                    .toList();
            Table suitRow = new Table(skin);
            if (group.isEmpty()) {
                suitRow.add(CardView.empty(assets)).pad(2f);
            }
            for (Card c : group) {
                CardView view = CardView.face(c, assets,
                        card -> input.onCardClicked(card));
                if (awaiting && valid.contains(c)) {
                    view.markPlayable();
                }
                suitRow.add(view).pad(2f);
            }
            humanHandTable.add(suitRow).center().row();
        }

        // Human trump label under the hand.
        humanHandTable.row();
        humanHandTable.add(new Label("Your trump: " + Card.Suit.SPADES.name(), skin))
                .center();
    }
}
