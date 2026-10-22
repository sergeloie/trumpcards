plugins {
    application
}

// Console platform module: the text front end of the new engine. It holds the
// strings of the console and the code that shows them and asks for a choice.
dependencies {
    implementation(project(":engine"))
    implementation(project(":bots"))

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
