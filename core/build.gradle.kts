// :core is the pure game engine. It has no main entry point — platform modules
// (console, desktop-libgdx, ...) provide the composition root.
plugins {
    `java-library`
}

dependencies {
    implementation(libs.guava)
    implementation(libs.gson)
    implementation(libs.lombok)
    annotationProcessor(libs.lombok)

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
