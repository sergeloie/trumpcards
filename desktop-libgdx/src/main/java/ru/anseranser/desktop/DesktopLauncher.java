package ru.anseranser.desktop;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

/**
 * LibGDX desktop entry point (Windows / Linux / macOS).
 *
 * <p>Stage 1 of the desktop port: a minimal window that confirms the module
 * builds and links against the {@code :core} engine. The actual rendering
 * (a {@code GameListener} drawing events) and input (a click-driven
 * {@code InputProvider}) plus the {@code GameDriver} loop are added in the
 * next stages — the engine itself is reused unchanged.</p>
 */
public class DesktopLauncher extends ApplicationAdapter {

    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Trumpcards");
        config.setWindowedMode(800, 600);
        config.setResizable(true);
        new Lwjgl3Application(new DesktopLauncher(), config);
    }
}
