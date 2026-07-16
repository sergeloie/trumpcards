plugins {
    application
}

// Console platform module: a thin composition root that wires the :core engine
// to console-based input/output. Contains no game rules — only platform glue.
dependencies {
    implementation(project(":core"))

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

application {
    mainClass = "ru.anseranser.console.ConsoleLauncher"
}
