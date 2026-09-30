// :core is the pure game engine. It has no main entry point — platform modules
// (console, desktop-libgdx, ...) provide the composition root.
plugins {
    `java-library`
}

dependencies {
    implementation(libs.gson)
    // Lombok is a compile-time-only annotation processor: its classes must never
    // reach the runtime classpath (R8 in full mode would choke on the missing
    // lombok.javac.handlers.* references pulled in through ServiceLoader analysis).
    compileOnly(libs.lombok)
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
