package ru.anseranser.desktop;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import ru.anseranser.engine.Card;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

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
 * The human seat (chosen by the composition root, see {@link DesktopLauncher#HUMAN_SEAT})
 * sits at the bottom. Opponents are drawn as a single face-down card back
 * (rubashka) with the number of cards in their hand and their trump suit, placed
 * LEFT / TOP / RIGHT according to turn order. The pot
 * sits in the middle. All cards and backs are shown vertically; the left and
 * right opponents' cards are rotated 90° so their long edge faces the centre.
 *
 * <p>The screen is stateless w.r.t. the engine: {@link #render(GameSnapshot)}
 * rebuilds the widgets from an immutable {@link GameSnapshot} (captured on the
 * engine thread) every time a transition is rendered. All widget mutation
 * happens on the LibGDX render thread.</p>
 *
 * <p>The cards of the hand that the engine lists as legal are marked green, so
 * the person sees what may be played. The button that takes the pot is enabled
 * when taking the pot is one of the legal moves.</p>
 */
final class GameScreen extends Table {

    /** The number of lines of the log the screen shows. */
    private static final int LOG_LINES = 8;

    private final Skin skin;
    private final DesktopInputProvider input;
    private final CardAssets assets;

    private final Label statusLabel;
    private final VerticalGroup logGroup;
    private final Table potTable;
    private final Table humanHandTable;
    private final VerticalGroup scoreboardGroup;
    private final Table leftOpponent;
    private final Table topOpponent;
    private final Table rightOpponent;
    private final Table humanPanel;
    private final TextButton takePotButton;

    GameScreen(Skin skin, DesktopInputProvider input, CardAssets assets) {
        super(skin);
        this.skin = skin;
        this.input = input;
        this.assets = assets;

        statusLabel = new Label("", skin);
        logGroup = new VerticalGroup();
        potTable = new Table(skin);
        humanHandTable = new Table(skin);
        scoreboardGroup = new VerticalGroup();
        leftOpponent = new Table(skin);
        topOpponent = new Table(skin);
        rightOpponent = new Table(skin);
        humanPanel = new Table(skin);
        takePotButton = new TextButton("TAKE POT", skin);
        takePotButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                input.onTakePotClicked();
            }
        });

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
        humanPanel.row();
        humanPanel.add(takePotButton).center();

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

        // Log of the events (above the status line) and the status line, both
        // pinned to the very bottom-left.
        Table bottom = new Table(skin);
        bottom.add(logGroup).left().row();
        bottom.add(statusLabel).left();
        add(bottom).colspan(3).left().bottom();
    }

    /**
     * Rebuild all widgets from an immutable {@link GameSnapshot} captured on the
     * engine thread. Must be invoked from the LibGDX render thread only.
     */
    void render(GameSnapshot snap) {
        statusLabel.setText(snap.status());
        takePotButton.setDisabled(!snap.takePotAvailable());

        // Log of the events, oldest first.
        logGroup.clear();
        List<String> lines = snap.logLines();
        for (String line : lines.subList(Math.max(0, lines.size() - LOG_LINES), lines.size())) {
            logGroup.addActor(new Label(line, skin));
        }

        // Scoreboard (top-left): most recent card on each suit's ladder.
        scoreboardGroup.clear();
        snap.scoreboard().forEach((suit, board) -> {
            String label = board.isEmpty()
                    ? "-"
                    : CardCodes.of(board.get(board.size() - 1));
            scoreboardGroup.addActor(new Label(label, skin));
        });

        // Opponents: one face-down card back (rotated for side seats) + card count
        // + trump suit, placed by seat.
        leftOpponent.clear();
        topOpponent.clear();
        rightOpponent.clear();
        for (GameSnapshot.OpponentView o : snap.opponents()) {
            Table panel = switch (o.position()) {
                case LEFT -> leftOpponent;
                case TOP -> topOpponent;
                case RIGHT -> rightOpponent;
            };
            CardView back = CardView.back(o.trump(), assets);
            if (o.position() == GameSnapshot.SeatPosition.LEFT) {
                back.setCardRotation(90f);
            } else if (o.position() == GameSnapshot.SeatPosition.RIGHT) {
                back.setCardRotation(-90f);
            }
            panel.add(back).pad(4f).row();
            panel.add(new Label(o.cardCount() + " cards", skin)).center().row();
            panel.add(new Label("Trump: " + o.trump().name(), skin)).center();
        }

        // Pot (center) — overlapping row so a large pot never overflows.
        potTable.clear();
        addOverlappingRow(potTable, snap.pot(), Set.of(), null);

        // Human hand (bottom) — a single horizontal row in Card.Suit declaration
        // order with cards sorted by rank inside each suit. Cards keep their true
        // size; when they would overflow the row width they overlap (stack)
        // instead of shrinking. The cards the engine lists as legal are marked.
        humanHandTable.clear();
        List<Card> hand = snap.humanHand().stream()
                .sorted(Comparator.comparingInt((Card c) -> c.suit().ordinal())
                        .thenComparingInt(c -> c.rank().ordinal()))
                .toList();
        addOverlappingRow(humanHandTable, hand, snap.playableCards(), input::onCardClicked);

        // Human trump label under the hand.
        humanHandTable.row();
        humanHandTable.add(new Label("Your trump: " + snap.humanSeat().name(), skin))
                .center();
    }

    /**
     * Lay out {@code cards} as a single horizontal row inside {@code row}, keeping
     * each card at its true size (no scaling). When the natural width (all cards
     * side by side with a small gap) would exceed {@code maxWidth}, the cards are
     * overlapped left-to-right by reducing the gap to a negative offset so the row
     * still fits. The cards of {@code playable} are marked; {@code onClick} is
     * the handler of a click on a card, or null when the cards are not clickable.
     */
    private void addOverlappingRow(Table row, List<Card> cards, Set<Card> playable,
                                    Consumer<Card> onClick) {
        if (cards.isEmpty()) {
            row.add(CardView.empty(assets)).pad(2f);
            return;
        }
        final float gap = 6f;
        final float maxWidth = 1820f; // leave margins inside the 1920 window
        float natural = cards.size() * CardView.CARD_W + (cards.size() - 1) * gap;
        float step = natural <= maxWidth
                ? CardView.CARD_W + gap
                : (maxWidth - CardView.CARD_W) / (cards.size() - 1);
        List<CardView> views = new ArrayList<>();
        boolean first = true;
        for (Card c : cards) {
            CardView view = CardView.face(c, assets, onClick == null ? null : onClick::accept);
            views.add(view);
            if (first) {
                row.add(view).pad(2f);
                first = false;
            } else {
                // Negative left pad pulls the next card on top of the previous one.
                row.add(view).pad(2f).padLeft(step - CardView.CARD_W - 4f);
            }
        }
        CardView.markLegal(views, playable);
    }
}
