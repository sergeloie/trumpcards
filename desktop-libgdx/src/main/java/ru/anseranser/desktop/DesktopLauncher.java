package ru.anseranser.desktop;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Timer;
import ru.anseranser.i18n.CardLocalizer;
import ru.anseranser.model.Game;
import ru.anseranser.model.Game.GameDriver;

/**
 * LibGDX desktop entry point (Windows / Linux / macOS).
 *
 * <p>Stage 2 wires the engine to a {@link GameScreen} with the requested
 * layout (human hand bottom-center, bank center, scoreboard top-left, move log
 * right). The game is auto-played through the engine's {@link GameDriver} with
 * a short pause between moves so the events stream into the listener and the
 * screen repaints. Interactive (click) input replaces the auto-player in
 * stage 3.</p>
 */
public class DesktopLauncher extends ApplicationAdapter {

    private Stage stage;
    private GameScreen screen;
    private Game game;
    private DesktopGameListener listener;

    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Trumpcards");
        config.setWindowedMode(960, 640);
        config.setResizable(true);
        new Lwjgl3Application(new DesktopLauncher(), config);
    }

    @Override
    public void create() {
        // Minimal skin built in code — no external asset file required.
        BitmapFont font = new BitmapFont();
        Skin skin = new Skin();
        skin.add("default", font, BitmapFont.class);
        skin.add("default", new Label.LabelStyle(font, com.badlogic.gdx.graphics.Color.BLACK), Label.LabelStyle.class);
        skin.add("default", new ScrollPane.ScrollPaneStyle(), ScrollPane.ScrollPaneStyle.class);

        screen = new GameScreen(skin, new CardLocalizer(CardLocalizer.Style.LETTERS));

        game = new Game(false);
        listener = new DesktopGameListener(game, screen);
        game.setListener(listener);

        stage = new Stage();
        stage.addActor(screen);
        Gdx.input.setInputProcessor(stage);

        // Auto-play the game move-by-move (interactive input comes in stage 3).
        GameDriver driver = game.createDriver();
        driver.startGame();
        scheduleStep(driver, 0.45f);
    }

    private void scheduleStep(GameDriver driver, float delay) {
        Timer.schedule(new Timer.Task() {
            @Override
            public void run() {
                if (driver.isGameOver()) {
                    return;
                }
                boolean more = driver.step();
                if (!more) {
                    driver.finishRound();
                }
                if (!driver.isGameOver()) {
                    scheduleStep(driver, delay);
                }
            }
        }, delay);
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
        stage.dispose();
    }
}
