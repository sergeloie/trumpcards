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
import ru.anseranser.i18n.CardLocalizer;
import ru.anseranser.input.HumanDecisionStrategy;
import ru.anseranser.model.AiDecisionStrategy;
import ru.anseranser.model.Card;
import ru.anseranser.model.DecisionStrategy;
import ru.anseranser.model.Game;
import ru.anseranser.model.Game.GameDriver;
import ru.anseranser.model.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * LibGDX desktop entry point (Windows / Linux / macOS).
 *
 * <p>Stage 3: the human plays by clicking cards. The engine's {@link GameDriver}
 * runs on a dedicated thread (NOT the LibGDX render thread) so that blocking on
 * a human choice does not freeze click handling. When it is an AI's turn the
 * loop sleeps briefly between moves for visibility; when it is the human's turn
 * {@link DesktopInputProvider#onCardClicked} (fired by a card click on the
 * render thread) resolves the blocked choice.</p>
 */
public class DesktopLauncher extends ApplicationAdapter {

    private static final float AI_PAUSE_MS = 450f;

    private Stage stage;
    private GameScreen screen;
    private CardAssets assets;
    private Game game;
    private DesktopGameListener listener;
    private DesktopInputProvider input;
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
        Skin skin = new Skin();
        skin.add("default-font", font, BitmapFont.class);
        skin.add("default", new Label.LabelStyle(font, Color.BLACK), Label.LabelStyle.class);
        skin.add("default", new ScrollPane.ScrollPaneStyle(), ScrollPane.ScrollPaneStyle.class);

        input = new DesktopInputProvider();
        assets = new CardAssets();
        screen = new GameScreen(skin, new CardLocalizer(CardLocalizer.Style.LETTERS), input, assets);

        // Composition root: SPADES seat is human (mouse-driven), the rest are AI.
        List<Player> players = new ArrayList<>();
        for (Card.Suit suit : Card.Suit.values()) {
            DecisionStrategy strategy = (suit == Card.Suit.SPADES)
                    ? new HumanDecisionStrategy(input)
                    : new AiDecisionStrategy();
            players.add(new Player(suit, strategy));
        }
        game = new Game(players);
        listener = new DesktopGameListener(game, screen);
        // Repaint when the human's valid choices become known (no engine event
        // is fired for "your turn"), so playable cards get highlighted.
        input.setRepaintHook(() -> listener.requestRepaint());
        game.setListener(listener);

        stage = new Stage();
        stage.addActor(screen);
        Gdx.input.setInputProcessor(stage);

        // Run the engine on its own thread so blocking on human input does not
        // stall the render thread that dispatches card clicks.
        gameThread = new Thread(this::runGame, "game-loop");
        gameThread.start();
    }

    private void runGame() {
        try {
            GameDriver driver = game.createDriver();
            driver.startGame();
            while (running && !driver.isGameOver()) {
                Player current = driver.getCurrent();
                boolean more = driver.step();
                if (!more) {
                    driver.finishRound();
                    continue;
                }
                // Only pause after an AI move; a human move already blocks until a click.
                if (!current.isHuman()) {
                    Thread.sleep((long) AI_PAUSE_MS);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
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
