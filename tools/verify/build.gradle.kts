// :tools:verify checks the invariants of the game: it plays many games and
// asserts that every card of the deck is in exactly one place at every step,
// that every game ends with exactly one seat left in it, and that a policy
// never chooses a move that is not legal. It reports the rounds that hit the
// safety bound instead of hiding them, and it can check an episode file the
// exporter wrote.
plugins {
    `java-library`
    application
}

dependencies {
    implementation(project(":core"))
    implementation(project(":bots"))

    // The episode files the exporter writes are read back and replayed here.
    implementation(project(":tools:exporter"))

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

application {
    mainClass = "ru.anseranser.verify.Verify"
}
