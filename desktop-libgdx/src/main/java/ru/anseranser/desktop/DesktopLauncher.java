package ru.anseranser.desktop;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import ru.anseranser.bots.RuleBot;
import ru.anseranser.engine.Card;
import ru.anseranser.engine.RankSet;
import ru.anseranser.engine.Setup;

import java.util.List;

/**
 * LibGDX desktop entry point (Windows / Linux / macOS).
 *
 * <p>The engine runs on a dedicated thread (NOT the LibGDX render thread) so
 * that blocking on a human choice does not freeze click handling. The engine
 * gives the legal moves, the person at the mouse picks one of them by clicking
 * a card, and the transition that follows is rendered on the render thread.</p>
 */
public class DesktopLauncher extends ApplicationAdapter {

    /** The pause after a move of the bot, so that the person sees it. */
    private static final long AI_PAUSE_MS = 450L;

    /**
     * The seat the human plays (finding D2). Decided once here, in the
     * composition root (R6), and passed down to the renderer/snapshot/screen so
     * the presentation layer never assumes a particular suit is the human.
     */
    static final Card.Suit HUMAN_SEAT = Card.Suit.SPADES;

    /** The suits in play, one per seat, in the seating order. */
    static final List<Card.Suit> TRUMPS = List.of(
            Card.Suit.SPADES, Card.Suit.CLUBS, Card.Suit.DIAMONDS, Card.Suit.HEARTS);

    private Stage stage;
    private Skin skin;
    private GameScreen screen;
    private CardAssets assets;
    private DesktopInputProvider input;
    private DesktopRenderer renderer;
    private Thread gameThread;
    private volatile boolean running = true;

    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Trumpcards");
        config.setWindowedMode(1920, 1080);
        config.setResizable(false);
        new Lwjgl3Application(new DesktopLauncher(), config);
    }

    @Override
    public void create() {
        // Minimal skin built in code — no external asset file required.
        // LibGDX resolves the default font under the name "default-font"; without
        // it a Label gets a null font and GlyphLayout throws NPE on any glyph.
        BitmapFont font = new BitmapFont();
        skin = new Skin();
        skin.add("default-font", font, BitmapFont.class);
        skin.add("default", new Label.LabelStyle(font, Color.BLACK), Label.LabelStyle.class);
        skin.add("default", new ScrollPane.ScrollPaneStyle(), ScrollPane.ScrollPaneStyle.class);
        // Minimal TextButton style (text only — no drawables) so the start menu
        // buttons can be built without an external skin asset. The disabled
        // colour tells the person that taking the pot is not a legal move.
        TextButton.TextButtonStyle textButtonStyle = new TextButton.TextButtonStyle();
        textButtonStyle.font = font;
        textButtonStyle.fontColor = Color.BLACK;
        textButtonStyle.disabledFontColor = Color.GRAY;
        skin.add("default", textButtonStyle, TextButton.TextButtonStyle.class);

        input = new DesktopInputProvider();
        assets = new CardAssets();
        screen = new GameScreen(skin, input, assets);

        stage = new Stage();
        Gdx.input.setInputProcessor(stage);

        // Show the start menu first; the engine starts only after the player
        // picks a deck size (36 or 52 cards).
        showStartMenu();
    }

    /** Display the pre-game deck-size menu. */
    private void showStartMenu() {
        stage.clear();
        stage.addActor(new StartMenu(skin, this::startGameWith));
    }

    /** Build the game for the chosen rank set and start the engine thread. */
    private void startGameWith(RankSet rankSet) {
        renderer = new DesktopRenderer(screen, HUMAN_SEAT);
        // Repaint when the human's legal moves become known, so that the
        // playable cards get highlighted.
        input.setRepaintHook(renderer::repaint);

        stage.clear();
        stage.addActor(screen);

        Setup setup = new Setup(rankSet, TRUMPS, System.nanoTime());
        MoveChooser bot = (state, moves) -> {
            waitForTheBot();
            return RuleBot.choose(state, moves);
        };
        GameLoop loop = new GameLoop(HUMAN_SEAT, input, bot, renderer::render);

        // Run the engine on its own thread so blocking on human input does not
        // stall the render thread that dispatches card clicks.
        gameThread = new Thread(() -> runGame(loop, setup), "game-loop");
        gameThread.start();
    }

    private void runGame(GameLoop loop, Setup setup) {
        try {
            loop.play(setup);
        } catch (RuntimeException e) {
            // A rules/engine failure should not silently freeze the UI; surface it
            // only when it is not just the expected interrupt during shutdown.
            if (running) {
                throw e;
            }
        } finally {
            running = false;
        }
    }

    private void waitForTheBot() {
        try {
            Thread.sleep(AI_PAUSE_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void render() {
        Gdx.gl.glClearColor(0.95f, 0.95f, 0.92f, 1f);
        Gdx.gl.glClear(Gdx.gl.GL_COLOR_BUFFER_BIT);
        stage.act(Gdx.graphics.getDeltaTime());
        stage.draw();
    }

    @Override
    public void dispose() {
        // Stop the engine thread (it is non-daemon and may be blocked waiting for
        // a human click); otherwise it keeps running against a disposed stage.
        running = false;
        if (gameThread != null) {
            gameThread.interrupt();
        }
        stage.dispose();
        assets.dispose();
    }
}
