plugins {
    application
}

// LibGDX desktop (Windows / Linux / macOS) backend.
// Depends only on the :core engine; rendering + input live here.
val gdxVersion = "1.14.2"

dependencies {
    implementation(project(":core"))

    implementation("com.badlogicgames.gdx:gdx:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-backend-lwjgl3:$gdxVersion")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

application {
    mainClass = "ru.anseranser.desktop.DesktopLauncher"
}
