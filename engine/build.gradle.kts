// :engine is the new rules module. It is built beside :core until the front
// ends are switched, and it depends on nothing but the JDK: plain records
// instead of Lombok, and nothing that would reach a runtime classpath.
plugins {
    `java-library`
}

dependencies {
    testImplementation(libs.junit.jupiter)
    // The golden gate compares the new engine with the old one. The old engine
    // is visible to the tests only; the main classpath stays empty.
    testImplementation(project(":core"))
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
