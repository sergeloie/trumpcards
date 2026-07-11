package ru.anseranser.desktop;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup;
import ru.anseranser.i18n.CardLocalizer;
import ru.anseranser.model.Card;
import ru.anseranser.model.Game;
import ru.anseranser.model.Player;

import java.util.List;

/**
 * Renders the game state with the layout requested for the desktop port:
 *
 * <pre>
 *  +----------------+-----------------------------+
 *  | scoreboard     |  (opponents hands, top)     |
 *  | (top-left)     |                             |
 *  |                +-----------------------------+
 *  |                |  BANK (center of screen)    |
 *  +----------------+-----------------------------+
 *  | move-log (right, full height)  | human hand  |
 *  |                                | (bottom, centered) |
 *  +--------------------------------+-------------+
 * </pre>
 *
 * The screen is stateless w.r.t. the engine: {@link #refresh(Game)} rebuilds
 * the widgets from the current {@link Game} snapshot every time an event fires.
 */
final class GameScreen extends Table {

    private final Skin skin;
    private final CardLocalizer localizer;
    private final DesktopInputProvider input;

    private final Label statusLabel;
    private final Table bankTable;
    private final Table humanHandTable;
    private final VerticalGroup scoreboardGroup;
    private final VerticalGroup opponentsGroup;
    private final VerticalGroup logGroup;

    GameScreen(Skin skin, CardLocalizer localizer, DesktopInputProvider input) {
        super(skin);
        this.skin = skin;
        this.localizer = localizer;
        this.input = input;

        statusLabel = new Label("", skin);
        bankTable = new Table(skin);
        humanHandTable = new Table(skin);
        scoreboardGroup = new VerticalGroup();
        opponentsGroup = new VerticalGroup();
        logGroup = new VerticalGroup();

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

        // Center: bank
        Table centerRow = new Table(skin);
        centerRow.add(new Label("BANK", skin)).row();
        centerRow.add(bankTable);

        // Bottom: human hand (left/center) + log (right)
        ScrollPane logPane = new ScrollPane(logGroup, skin);
        logPane.setFadeScrollBars(false);

        Table bottomRow = new Table(skin);
        bottomRow.add(new Label("YOUR HAND", skin)).colspan(2).center().row();
        bottomRow.add(humanHandTable).expandX().center();
        bottomRow.add(logPane).width(280f).top().right();

        add(statusLabel).colspan(2).left().row();
        add(topRow).colspan(2).expandX().fillX().top().row();
        add(centerRow).colspan(2).expand().top().row();
        add(bottomRow).colspan(2).expandX().fillX().bottom();
    }

    /** Rebuild all widgets from the current game snapshot. Called on every event. */
    void refresh(Game game) {
        statusLabel.setText("Trump: " + game.getTrump() + "   Players left: "
                + countGamers(game));

        // Scoreboard (left, top)
        scoreboardGroup.clear();
        game.getScoreboard().snapshot().forEach((suit, stack) -> {
            StringBuilder sb = new StringBuilder(localizer.suitName(suit) + ": ");
            for (Card c : stack) {
                sb.append(localizer.cardName(c, CardLocalizer.Style.LETTERS)).append(" ");
            }
            scoreboardGroup.addActor(new Label(sb.toString(), skin));
        });

        // Opponents (top-right): every gamer except the human seat (SPADES)
        opponentsGroup.clear();
        for (Player p : game.getPlayers()) {
            if (p.isGamer() && p.getTrump() != Card.Suit.SPADES) {
                Label l = new Label(p + "  (" + p.getHand().size() + " cards)", skin);
                opponentsGroup.addActor(l);
            }
        }

        // Bank (center)
        bankTable.clear();
        for (Card c : game.getBank()) {
            bankTable.add(new CardView(c, skin, localizer)).pad(2f);
        }

        // Human hand (bottom, centered) — cards are clickable while awaiting input.
        humanHandTable.clear();
        Player human = findHuman(game);
        if (human != null) {
            List<Card> valid = input.validChoices();
            boolean awaiting = !valid.isEmpty();
            for (Card c : human.getHand()) {
                CardView view = new CardView(c, skin, localizer,
                        card -> input.onCardClicked(card));
                if (awaiting && valid.contains(c)) {
                    view.getActor().setColor(Color.GREEN);
                }
                humanHandTable.add(view).pad(2f);
            }
        }
    }

    /** Replace the move log (called by the listener after it records a line). */
    void setLog(List<String> lines) {
        logGroup.clear();
        for (String line : lines) {
            Label l = new Label(line, skin);
            l.setWrap(true);
            logGroup.addActor(l);
        }
    }

    private static int countGamers(Game game) {
        int n = 0;
        for (Player p : game.getPlayers()) {
            if (p.isGamer()) n++;
        }
        return n;
    }

    private static Player findHuman(Game game) {
        for (Player p : game.getPlayers()) {
            if (p.getTrump() == Card.Suit.SPADES && p.isGamer()) {
                return p;
            }
        }
        return null;
    }
}
