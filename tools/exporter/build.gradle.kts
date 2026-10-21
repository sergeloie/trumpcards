// :tools:exporter writes the training episodes for a trainer in another
// language: one game per line, as JSONL. It depends on the rules, on the
// policies and on the observation, and it owns the format of the file it
// writes. The rules know nothing of it: the format belongs to the writer.
plugins {
    `java-library`
    application
}

dependencies {
    implementation(project(":engine"))
    implementation(project(":bots"))
    implementation(project(":ml"))

    // The file is JSON, and the rules have no serialisation dependency.
    implementation(libs.gson)

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
    mainClass = "ru.anseranser.exporter.Exporter"
}
