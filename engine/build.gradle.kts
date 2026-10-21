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
    // The gate drives the new engine with the rule bot of the :bots module.
    // The two modules depend on each other in the test scope only: the bot
    // needs the engine's types, the gate needs the bot. The task graph stays
    // acyclic, because :engine:compileTestJava needs :bots:jar, and :bots:jar
    // needs :engine:jar.
    testImplementation(project(":bots"))
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
