package ru.anseranser.desktop;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;

import ru.anseranser.model.DeckSize;

import java.util.function.Consumer;

/**
 * Pre-game menu shown by the desktop launcher. It lets the player pick the deck
 * size (36 or 52 cards) before the engine starts. On selection it invokes the
 * supplied callback with the chosen {@link DeckSize}; the launcher then builds
 * the {@code Game} and starts the engine thread.
 *
 * <p>This is a launch-time UI concern (not an in-game decision), so it lives in
 * the desktop presentation layer rather than the {@code InputProvider} seam.</p>
 */
final class StartMenu extends Table {

    StartMenu(Skin skin, Consumer<DeckSize> onChoose) {
        super(skin);
        setFillParent(true);
        pad(16f);

        Label title = new Label("Trumpcards", skin);
        add(title).center().row();

        Label prompt = new Label("Choose deck size", skin);
        add(prompt).center().padTop(24f).row();

        TextButton thirtySix = new TextButton("36 cards", skin);
        thirtySix.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                onChoose.accept(DeckSize.THIRTY_SIX);
            }
        });

        TextButton fiftyTwo = new TextButton("52 cards", skin);
        fiftyTwo.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                onChoose.accept(DeckSize.FIFTY_TWO);
            }
        });

        Table buttons = new Table(skin);
        buttons.add(thirtySix).pad(8f);
        buttons.add(fiftyTwo).pad(8f);
        add(buttons).center().padTop(16f);
    }
}
